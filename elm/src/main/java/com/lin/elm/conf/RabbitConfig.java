package com.lin.elm.conf;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置：秒杀下单队列 + 死信队列
 *
 * 只有 seckill.mq-enabled=true 时才会创建这些 Bean，
 * 这样没装 RabbitMQ 的环境也能正常启动（走同步下单）
 */
@Configuration
@ConditionalOnProperty(name = "seckill.mq-enabled", havingValue = "true")
public class RabbitConfig {

    /**
     * 秒杀下单队列
     *
     * 注意：统一加 elm_ 前缀做命名空间隔离。
     * 因为 RabbitMQ 规定「重复声明已存在的队列时参数必须完全一致」，否则报
     * 406 PRECONDITION_FAILED；而本机 RabbitMQ 里已经有一个别的项目
     * （xiaomi）创建的 seckill_order 队列，它的死信交换机名字和我们的不一样，
     * 用同名会一直报错，所以这里用 elm_ 前缀把两个项目隔开。
     */
    public static final String SECKILL_QUEUE = "elm_seckill_order";
    /** 死信交换机 / 死信队列：消费失败（比如重试超限）的消息会进这里，方便人工排查 */
    public static final String SECKILL_DLX_EXCHANGE = "elm_seckill_dlx_exchange";
    public static final String SECKILL_DLX_QUEUE = "elm_seckill_dead_letter_queue";
    public static final String SECKILL_ROUTING_KEY = "elm.seckill.order";
    public static final String SECKILL_DLX_ROUTING_KEY = "elm.seckill.dlx";

    @Bean
    public Queue seckillQueue() {
        return QueueBuilder.durable(SECKILL_QUEUE)
                // 消费失败被 reject 的消息，自动转发到死信交换机
                .deadLetterExchange(SECKILL_DLX_EXCHANGE)
                .deadLetterRoutingKey(SECKILL_DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange seckillDlxExchange() {
        return new DirectExchange(SECKILL_DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue seckillDeadLetterQueue() {
        return QueueBuilder.durable(SECKILL_DLX_QUEUE).build();
    }

    @Bean
    public Binding seckillDeadLetterBinding() {
        return BindingBuilder.bind(seckillDeadLetterQueue())
                .to(seckillDlxExchange())
                .with(SECKILL_DLX_ROUTING_KEY);
    }

    /**
     * 消息用 JSON 序列化，方便在 RabbitMQ 管理界面直接看到消息内容
     * 受信任包只放开我们自己的 PO：反序列化时不会因为消息头里的类型而被反序列化成任意类（防反序列化漏洞）
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("com.lin.elm.po", "java.util", "java.lang", "java.math");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
