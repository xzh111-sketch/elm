package com.lin.elm.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 秒杀请求消息（发到 RabbitMQ 的内容）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillMessage implements Serializable {
    /** 消息唯一编号，用于幂等 */
    private String messageId;
    private Integer seckillId;
    private String userId;
    private Double seckillPrice;
}
