package com.lin.elm.service.Impl;

import com.lin.elm.conf.RabbitConfig;
import com.lin.elm.mapper.DeliveryAddressMapper;
import com.lin.elm.mapper.SeckillMapper;
import com.lin.elm.mapper.SeckillOrderMapper;
import com.lin.elm.po.DeliveryAddress;
import com.lin.elm.po.Seckill;
import com.lin.elm.po.SeckillMessage;
import com.lin.elm.po.SeckillOrder;
import com.lin.elm.service.SeckillOrderHandler;
import com.lin.elm.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 秒杀核心逻辑
 *
 * 整体流程：
 *   1. 校验活动（存在、上架、在时间范围内、用户有收货地址）
 *   2. 执行 Redis Lua 脚本：原子地「判断是否重复购买 + 判断库存 + 扣减库存」
 *      —— 这一步把 90% 的无效请求挡在数据库之外，数据库压力不随并发增长
 *   3. Redis 扣减成功 → 发 MQ 消息（异步削峰），真正下单交给消费者
 *   4. 消费者里再用「update ... where stock > 0」和唯一索引兜底，保证不超卖、不重复
 */
@Service
public class SeckillServiceImpl implements SeckillService, ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeckillServiceImpl.class);

    @Autowired
    private SeckillMapper seckillMapper;
    @Autowired
    private SeckillOrderMapper seckillOrderMapper;
    @Autowired
    private DeliveryAddressMapper deliveryAddressMapper;
    @Autowired
    private SeckillOrderHandler orderHandler;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private DefaultRedisScript<Long> seckillScript;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /** 是否走 RabbitMQ 异步下单 */
    @Value("${seckill.mq-enabled:false}")
    private boolean mqEnabled;

    private String stockKey(Integer seckillId) {
        return SeckillOrderHandler.STOCK_KEY_PREFIX + seckillId;
    }

    private String usersKey(Integer seckillId) {
        return SeckillOrderHandler.USERS_KEY_PREFIX + seckillId;
    }

    @Override
    public List<Seckill> listOnSale() {
        List<Seckill> list = seckillMapper.listOnSale();
        for (Seckill seckill : list) {
            try {
                String stock = redisTemplate.opsForValue().get(stockKey(seckill.getSeckillId()));
                // Redis 里有就用 Redis 的实时库存，没有就先用数据库的
                seckill.setRedisStock(stock == null
                        ? seckill.getSeckillStock()
                        : Integer.valueOf(stock));
            } catch (Exception e) {
                // Redis 不可用时用数据库库存兜底，保证秒杀页还能打开
                log.warn("读取 Redis 库存失败，改用数据库库存展示：{}", e.getMessage());
                seckill.setRedisStock(seckill.getSeckillStock());
            }
        }
        return list;
    }

    @Override
    public Map<String, Object> doSeckill(String userId, Integer seckillId) {
        // 1. 活动校验
        Seckill seckill = seckillMapper.getById(seckillId);
        if (seckill == null || seckill.getStatus() == null || seckill.getStatus() != 1) {
            return fail("秒杀活动不存在或已下架");
        }
        if (!inTimeRange(seckill)) {
            return fail("不在秒杀时间范围内");
        }
        // 2. 下单需要收货地址
        List<DeliveryAddress> addresses = deliveryAddressMapper.listDeliveryAddressByUserId(userId);
        if (addresses == null || addresses.isEmpty()) {
            return fail("请先添加收货地址再参与秒杀");
        }

        // 3. 预热 + 4. 执行 Lua（Redis 不可用时给出友好提示，避免 500）
        Long result;
        try {
            // 预热：Redis 里没有该活动的库存就从数据库灌一次（只在 key 不存在时写，避免覆盖已扣减的库存）
            redisTemplate.opsForValue()
                    .setIfAbsent(stockKey(seckillId), String.valueOf(seckill.getSeckillStock()));
            // 执行 Lua：一次原子操作完成「重复购买判断 + 库存判断 + 扣库存」
            result = redisTemplate.execute(
                    seckillScript,
                    Arrays.asList(stockKey(seckillId), usersKey(seckillId)),
                    userId);
        } catch (Exception e) {
            log.error("秒杀依赖的 Redis 不可用：{}", e.getMessage());
            return fail("秒杀服务暂时不可用（请确认 Redis 已启动）");
        }

        if (result == null) {
            return fail("秒杀失败，请重试");
        }
        if (result == -1L) {
            return fail("每人限购 1 件，您已经抢购过了");
        }
        if (result == 0L) {
            return fail("已抢光，下次早点来～");
        }

        // 5. Redis 预扣成功，开始生成订单
        SeckillMessage message = new SeckillMessage(
                seckillId + ":" + userId + ":" + System.currentTimeMillis(),
                seckillId, userId, seckill.getSeckillPrice());

        if (mqEnabled) {
            try {
                // 异步削峰：先返回"排队中"，真正的下单由消费者慢慢处理
                rabbitTemplate.convertAndSend(RabbitConfig.SECKILL_QUEUE, message);
                Map<String, Object> queuing = new HashMap<>();
                queuing.put("success", true);
                queuing.put("status", "QUEUING");
                queuing.put("message", "抢购成功，正在为您生成订单...");
                return queuing;
            } catch (Exception e) {
                // MQ 不可用时不要直接让用户失败，降级为同步下单
                log.warn("RabbitMQ 不可用，降级为同步下单：{}", e.getMessage());
            }
        }

        // 同步下单（mq-enabled=false，或者 MQ 发送失败时的兜底）
        try {
            orderHandler.handle(message);
        } catch (Exception e) {
            log.error("同步下单失败：{}", message, e);
            orderHandler.compensateRedis(message);
            return fail("下单失败，请重试");
        }
        return getResult(userId, seckillId);
    }

    @Override
    public Map<String, Object> getResult(String userId, Integer seckillId) {
        SeckillOrder record = seckillOrderMapper.getByUserAndSeckill(seckillId, userId);
        Map<String, Object> result = new HashMap<>();
        if (record == null) {
            result.put("success", false);
            result.put("status", "NONE");
            result.put("message", "您还没有参与该秒杀");
            return result;
        }
        Integer status = record.getStatus() == null ? 0 : record.getStatus();
        result.put("status", status == 0 ? "QUEUING" : (status == 1 ? "SUCCESS" : "FAILED"));
        result.put("success", status == 1);
        result.put("orderId", record.getOrderId());
        result.put("message", status == 0 ? "订单生成中..." : (status == 1 ? "抢购成功" : "抢购失败"));
        return result;
    }

    @Override
    public int preheatStock() {
        int count = 0;
        for (Seckill seckill : seckillMapper.listOnSale()) {
            Boolean absent = redisTemplate.opsForValue()
                    .setIfAbsent(stockKey(seckill.getSeckillId()), String.valueOf(seckill.getSeckillStock()));
            if (Boolean.TRUE.equals(absent)) {
                count++;
            }
        }
        log.info("秒杀库存预热完成，新灌入 {} 个活动的库存到 Redis", count);
        return count;
    }

    @Override
    public int resetStock() {
        List<Seckill> list = seckillMapper.listOnSale();
        for (Seckill seckill : list) {
            // 用数据库库存覆盖 Redis，并清掉"已购用户"集合
            redisTemplate.opsForValue().set(stockKey(seckill.getSeckillId()),
                    String.valueOf(seckill.getSeckillStock()));
            redisTemplate.delete(usersKey(seckill.getSeckillId()));
        }
        log.info("已把 {} 个秒杀活动的库存重置到 Redis", list.size());
        return list.size();
    }

    /** 应用启动时预热一次（Redis 没启动也不会影响应用启动） */
    @Override
    public void run(ApplicationArguments args) {
        try {
            preheatStock();
        } catch (Exception e) {
            log.warn("秒杀库存预热失败（Redis 可能没启动）：{}", e.getMessage());
        }
    }

    private boolean inTimeRange(Seckill seckill) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            long now = System.currentTimeMillis();
            return now >= sdf.parse(seckill.getStartTime()).getTime()
                    && now <= sdf.parse(seckill.getEndTime()).getTime();
        } catch (Exception e) {
            // 时间格式异常时不拦，避免影响主流程
            return true;
        }
    }

    private Map<String, Object> fail(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("status", "FAILED");
        result.put("message", message);
        return result;
    }
}
