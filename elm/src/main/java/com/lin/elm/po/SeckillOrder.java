package com.lin.elm.po;

import lombok.Data;

/**
 * 秒杀参与记录
 * 表上有唯一索引 (seckillId, userId)，是防重复购买的最后一道防线
 */
@Data
public class SeckillOrder {
    private Long soId;
    private Integer seckillId;
    private String userId;
    /** 异步入库后生成的正式订单号 */
    private Integer orderId;
    private Double seckillPrice;
    private String createTime;
    /** 0 排队中 1 已成单 2 失败 */
    private Integer status;
}
