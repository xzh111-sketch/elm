package com.lin.elm.controller;

import com.lin.elm.po.Orders;
import com.lin.elm.service.OrdersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("OrdersController")
public class OrdersController {
    @Autowired
    private OrdersService ordersService;

    /** 下单：userId 以 token 为准，防止给别人的账号下单 */
    @RequestMapping("/saveOrders")
    public int saveOrders(Orders orders, HttpServletRequest request){
        orders.setUserId((String) request.getAttribute("userId"));
        return ordersService.saveOrders(orders);
    }

    /** 查单：只能查自己的订单，别人的订单一律返回 null */
    @RequestMapping("/getOrdersById")
    public Orders getOrdersById(Integer orderId, HttpServletRequest request){
        Orders orders = ordersService.getOrdersById(orderId);
        String userId = (String) request.getAttribute("userId");
        if (orders != null && userId != null && !userId.equals(orders.getUserId())) {
            return null;
        }
        return orders;
    }

    /**
     * 订单列表（分页 + 状态筛选）：默认第 1 页、每页 5 条
     * orderState 不传 = 全部；传 0 = 未支付；传 1 = 已支付
     * 返回 {list: [...], total: 总条数, pageNum, pageSize, hasMore: 是否还有下一页}
     */
    @RequestMapping("/listOrdersByUserId")
    public Map<String, Object> listOrdersByUserId(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "5") Integer pageSize,
            @RequestParam(value = "orderState", required = false) Integer orderState,
            HttpServletRequest request){
        // 防止前端乱传导致全表扫描
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1 || pageSize > 50) {
            pageSize = 5;
        }
        String userId = (String) request.getAttribute("userId");
        int offset = (pageNum - 1) * pageSize;

        List<Orders> list = ordersService.listOrdersByUserId(userId, orderState, offset, pageSize);
        int total = ordersService.countOrdersByUserId(userId, orderState);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("orderState", orderState);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        result.put("hasMore", offset + list.size() < total);
        return result;
    }
}
