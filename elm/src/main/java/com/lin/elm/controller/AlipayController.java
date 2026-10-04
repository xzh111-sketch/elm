package com.lin.elm.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.lin.elm.po.Orders;
import com.lin.elm.service.OrdersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 支付宝沙箱支付
 * 1) GET  /api/alipay/pay?orderId=x  生成收银台表单，前端提交后跳转支付宝
 * 2) POST /api/alipay/notify        支付宝服务器异步通知，验签通过后把订单置为已支付
 *
 * 重要：支付结果的唯一权威来源是 notify（支付宝服务器发起、带签名）。
 *      return_url 只是给用户看的跳转页，谁都能手动访问，绝不能作为支付成功的依据。
 */
@RestController
@RequestMapping("/api/alipay")
public class AlipayController {

    private static final Logger log = LoggerFactory.getLogger(AlipayController.class);

    @Value("${alipay.app-id}")
    private String appId;
    @Value("${alipay.private-key}")
    private String privateKey;
    @Value("${alipay.alipay-public-key}")
    private String alipayPublicKey;
    @Value("${alipay.gateway-url}")
    private String gatewayUrl;
    @Value("${alipay.notify-url}")
    private String notifyUrl;
    @Value("${alipay.return-url}")
    private String returnUrl;

    @Autowired
    private OrdersService ordersService;

    /** 支付宝客户端是线程安全的，创建一次复用即可 */
    private AlipayClient alipayClient;

    private AlipayClient client() {
        if (alipayClient == null) {
            alipayClient = new DefaultAlipayClient(
                    gatewayUrl, appId, privateKey,
                    "json", "UTF-8", alipayPublicKey, "RSA2");
        }
        return alipayClient;
    }

    /**
     * 生成支付宝收银台页面：返回一段自带 form 的 HTML，前端拿到后直接 submit
     */
    @GetMapping(value = "/pay", produces = "text/html;charset=UTF-8")
    public String pay(@RequestParam("orderId") Integer orderId, HttpServletRequest request) {
        String currentUserId = (String) request.getAttribute("userId");
        Orders order = ordersService.getOrdersById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在：" + orderId);
        }
        // 只能为自己的订单发起支付（userId 来自 token，不信任前端传参）
        if (currentUserId != null && !currentUserId.equals(order.getUserId())) {
            throw new RuntimeException("无权支付他人订单");
        }
        if (order.getOrderState() != null && order.getOrderState() == 1) {
            return "<h3>该订单已完成支付，请勿重复支付</h3>";
        }

        AlipayTradePagePayRequest payRequest = new AlipayTradePagePayRequest();
        payRequest.setNotifyUrl(notifyUrl);   // 公网地址，支付宝服务器回调
        payRequest.setReturnUrl(returnUrl);   // 用户付款后浏览器跳回这里

        // out_trade_no 用订单号保证唯一；total_amount 单位是元，必须是两位小数字符串
        String bizContent = "{"
                + "\"out_trade_no\":\"" + order.getOrderId() + "\","
                + "\"total_amount\":\"" + String.format(Locale.ROOT, "%.2f", order.getOrderTotal()) + "\","
                + "\"subject\":\"饿了么外卖订单-" + order.getOrderId() + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\""
                + "}";
        payRequest.setBizContent(bizContent);

        try {
            return client().pageExecute(payRequest).getBody();
        } catch (AlipayApiException e) {
            log.error("生成支付宝支付页面失败", e);
            throw new RuntimeException("生成支付页面失败，请检查支付宝配置：" + e.getMessage());
        }
    }

    /**
     * 支付宝异步通知
     * 处理顺序：验签 -> 校验订单与金额 -> 幂等判断 -> 更新订单状态
     * 返回纯文本 success 表示"我收到了，别再通知了"；返回其他内容支付宝会反复重试
     */
    @PostMapping(value = "/notify", produces = "text/plain;charset=UTF-8")
    public String notify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            StringBuilder value = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                value.append(i == 0 ? values[i] : "," + values[i]);
            }
            params.put(name, value.toString());
        }
        log.info("收到支付宝异步通知：{}", params);

        // 1. 验签：用支付宝公钥验证签名，确认请求确实来自支付宝（最关键的一步）
        try {
            if (!AlipaySignature.rsaCheckV1(params, alipayPublicKey, "UTF-8", "RSA2")) {
                log.warn("支付宝异步通知验签失败");
                return "fail";
            }
        } catch (AlipayApiException e) {
            log.error("支付宝异步通知验签异常", e);
            return "fail";
        }

        String outTradeNo = params.get("out_trade_no");
        String totalAmount = params.get("total_amount");
        String tradeNo = params.get("trade_no");
        String tradeStatus = params.get("trade_status");
        if (outTradeNo == null) {
            return "fail";
        }

        Orders order;
        try {
            order = ordersService.getOrdersById(Integer.valueOf(outTradeNo));
        } catch (NumberFormatException e) {
            log.warn("异步通知里的订单号格式不正确：{}", outTradeNo);
            return "fail";
        }

        // 2. 校验订单存在、金额一致（防止金额被篡改）
        if (order == null) {
            log.warn("异步通知对应的订单不存在：{}", outTradeNo);
            return "fail";
        }
        if (totalAmount == null
                || new BigDecimal(totalAmount).compareTo(BigDecimal.valueOf(order.getOrderTotal())) != 0) {
            log.warn("订单金额与通知金额不一致：订单={} 通知={}", order.getOrderTotal(), totalAmount);
            return "fail";
        }

        // 3. 幂等：同一订单可能收到多次通知，已支付就直接回 success，避免重复处理
        if (order.getOrderState() != null && order.getOrderState() == 1) {
            log.info("订单 {} 已支付，忽略重复通知", outTradeNo);
            return "success";
        }

        // 4. 只有这两个状态才代表钱真的到账
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            ordersService.updateOrderState(order.getOrderId(), 1);
            ordersService.updateTradeNo(order.getOrderId(), tradeNo);
            log.info("订单 {} 支付成功，支付宝交易号：{}", outTradeNo, tradeNo);
        }
        return "success";
    }
}
