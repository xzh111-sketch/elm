package com.lin.elm.service;

import com.lin.elm.conf.CommonUtil;
import com.lin.elm.mapper.DeliveryAddressMapper;
import com.lin.elm.mapper.OrderDetailetMapper;
import com.lin.elm.mapper.OrdersMapper;
import com.lin.elm.mapper.SeckillMapper;
import com.lin.elm.mapper.SeckillOrderMapper;
import com.lin.elm.po.DeliveryAddress;
import com.lin.elm.po.OrderDetailet;
import com.lin.elm.po.Orders;
import com.lin.elm.po.Seckill;
import com.lin.elm.po.SeckillMessage;
import com.lin.elm.po.SeckillOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 秒杀订单落地：把"Redis 预扣成功"变成真正的订单
 *
 * 为什么单独抽成一个 Bean（而不是写在 SeckillService 里）：
 * 1) @Transactional 依赖 Spring 代理，同一个类内部 this.handle() 调用不会走代理，事务会失效；
 * 2) MQ 消费者和"同步降级"两条路都要调用它，抽出来复用。
 */
@Service
public class SeckillOrderHandler {

    private static final Logger log = LoggerFactory.getLogger(SeckillOrderHandler.class);

    public static final String STOCK_KEY_PREFIX = "seckill:stock:";
    public static final String USERS_KEY_PREFIX = "seckill:users:";

    @Autowired
    private SeckillMapper seckillMapper;
    @Autowired
    private SeckillOrderMapper seckillOrderMapper;
    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private OrderDetailetMapper orderDetailetMapper;
    @Autowired
    private DeliveryAddressMapper deliveryAddressMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 真正下单（整体在一个事务里）
     * 任何一步抛异常，数据库的扣库存和插订单都会一起回滚
     */
    @Transactional(rollbackFor = Exception.class)
    public void handle(SeckillMessage msg) {
        // 1. 幂等：同一用户同一活动已经成单，直接返回（MQ 可能重复投递）
        SeckillOrder exist = seckillOrderMapper.getByUserAndSeckill(msg.getSeckillId(), msg.getUserId());
        if (exist != null && exist.getStatus() != null && exist.getStatus() == 1) {
            log.info("秒杀消息重复投递，忽略：{}", msg);
            return;
        }

        Seckill seckill = seckillMapper.getById(msg.getSeckillId());
        if (seckill == null) {
            log.warn("秒杀活动不存在：{}", msg);
            return;
        }

        // 2. 数据库扣库存：最后一道防线（stock > 0 才扣得动）
        int rows = seckillMapper.deductDbStock(msg.getSeckillId());
        if (rows == 0) {
            log.warn("数据库库存不足，回滚 Redis 预扣：{}", msg);
            compensateRedis(msg);
            if (exist != null) {
                seckillOrderMapper.markFailed(exist.getSoId());
            }
            return;
        }

        // 3. 记录秒杀流水（唯一索引 (seckillId,userId) 是防重复的最后一道防线）
        Long soId;
        if (exist == null) {
            SeckillOrder so = new SeckillOrder();
            so.setSeckillId(msg.getSeckillId());
            so.setUserId(msg.getUserId());
            so.setSeckillPrice(msg.getSeckillPrice());
            so.setCreateTime(CommonUtil.getCurrentDate());
            seckillOrderMapper.insert(so);
            soId = so.getSoId();
        } else {
            soId = exist.getSoId();
        }

        // 4. 生成正式订单（状态 0 = 待支付，之后可以走支付宝支付流程）
        Orders orders = new Orders();
        orders.setUserId(msg.getUserId());
        orders.setBusinessId(seckill.getBusinessId());
        orders.setOrderDate(CommonUtil.getCurrentDate());
        orders.setOrderTotal(seckill.getSeckillPrice());
        orders.setDaId(getFirstAddressId(msg.getUserId()));
        orders.setOrderState(0);
        ordersMapper.saveOrders(orders);

        // 订单明细：秒杀商品 1 件
        OrderDetailet od = new OrderDetailet();
        od.setOrderId(orders.getOrderId());
        od.setFoodId(seckill.getFoodId());
        od.setQuantity(1);
        orderDetailetMapper.saveOrderDetailet(Collections.singletonList(od));

        // 5. 回填正式订单号，标记成功
        seckillOrderMapper.markSuccess(soId, orders.getOrderId());
        log.info("秒杀下单成功：用户={} 活动={} 订单号={}", msg.getUserId(), msg.getSeckillId(), orders.getOrderId());
    }

    /**
     * 补偿：把 Redis 里预扣的库存和"已购标记"还回去
     * 注意 Redis 不参与数据库事务，所以失败时必须在业务层手动补偿，
     * 否则会出现"库存被吃掉但没生成订单"
     */
    public void compensateRedis(SeckillMessage msg) {
        try {
            redisTemplate.opsForValue().increment(STOCK_KEY_PREFIX + msg.getSeckillId());
            redisTemplate.opsForSet().remove(USERS_KEY_PREFIX + msg.getSeckillId(), msg.getUserId());
        } catch (Exception e) {
            log.error("Redis 补偿失败，需要人工核对：{}", msg, e);
        }
    }

    /** 秒杀订单用用户的第一个收货地址（真实项目一般让用户先选地址再抢购） */
    private Integer getFirstAddressId(String userId) {
        List<DeliveryAddress> list = deliveryAddressMapper.listDeliveryAddressByUserId(userId);
        if (list == null || list.isEmpty()) {
            throw new RuntimeException("用户没有收货地址，无法生成订单");
        }
        return list.get(0).getDaId();
    }
}
