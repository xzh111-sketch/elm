package com.lin.elm.mq;

import com.lin.elm.conf.RabbitConfig;
import com.lin.elm.po.SeckillMessage;
import com.lin.elm.service.SeckillOrderHandler;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀下单消费者：从队列里取消息、异步生成订单
 *
 * 三个关键点：
 * 1) 手动 ack：处理成功才确认，失败的消息不会被丢掉
 * 2) 重试：失败先重新入队，重试次数用 Redis 计数（避免无限重试把队列打爆）
 * 3) 死信队列：重试超限后不再入队，消息进死信队列，方便人工排查
 */
@Component
@ConditionalOnProperty(name = "seckill.mq-enabled", havingValue = "true")
public class SeckillOrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(SeckillOrderConsumer.class);
    private static final int MAX_RETRY = 3;
    private static final String RETRY_KEY_PREFIX = "seckill:retry:";

    @Autowired
    private SeckillOrderHandler orderHandler;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @RabbitListener(queues = RabbitConfig.SECKILL_QUEUE)
    public void onMessage(SeckillMessage msg, Channel channel, Message message) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            orderHandler.handle(msg);
            channel.basicAck(deliveryTag, false);
            log.info("秒杀消息消费成功：{}", msg.getMessageId());
        } catch (Exception e) {
            log.error("秒杀下单失败：{}", msg, e);
            // 业务失败要把 Redis 预扣还回去，否则库存会被"吃掉"但又没有订单
            orderHandler.compensateRedis(msg);
            String retryKey = RETRY_KEY_PREFIX + msg.getMessageId();
            Long retry = redisTemplate.opsForValue().increment(retryKey);
            redisTemplate.expire(retryKey, 1, TimeUnit.HOURS);
            if (retry != null && retry >= MAX_RETRY) {
                log.warn("重试 {} 次仍失败，转入死信队列：{}", retry, msg);
                channel.basicNack(deliveryTag, false, false);  // 不再入队 → 走死信交换机
            } else {
                channel.basicNack(deliveryTag, false, true);   // 重新入队，稍后重试
            }
        }
    }

    /** 死信队列消费者：这里只记录日志，真实项目会告警 + 落表人工处理 */
    @RabbitListener(queues = RabbitConfig.SECKILL_DLX_QUEUE)
    public void onDeadLetter(SeckillMessage msg) {
        log.error("【死信】秒杀消息最终处理失败，需要人工介入：{}", msg);
    }
}
