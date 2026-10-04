package com.lin.elm.service;

import com.lin.elm.po.Seckill;

import java.util.List;
import java.util.Map;

public interface SeckillService {

    /** 在售秒杀列表（带上 Redis 实时库存） */
    public List<Seckill> listOnSale();

    /** 参与秒杀：Redis 原子扣减 → 生成订单 */
    public Map<String, Object> doSeckill(String userId, Integer seckillId);

    /** 查询某个用户在某场秒杀的抢购结果 */
    public Map<String, Object> getResult(String userId, Integer seckillId);

    /** 把数据库库存预热到 Redis（只在 key 不存在时灌） */
    public int preheatStock();

    /** 强制用数据库库存重置 Redis（演示/压测用） */
    public int resetStock();
}
