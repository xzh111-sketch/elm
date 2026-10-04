package com.lin.elm.po;

import lombok.Data;

/**
 * 秒杀活动
 */
@Data
public class Seckill {
    private Integer seckillId;
    private Integer foodId;
    private Integer businessId;
    private Double seckillPrice;
    private Double originalPrice;
    /** 数据库里的库存（最终兜底） */
    private Integer seckillStock;
    private String startTime;
    private String endTime;
    private Integer status;

    /** Redis 里的实时剩余库存，只用于页面展示 */
    private Integer redisStock;

    private Food food;
    private Business business;
}
