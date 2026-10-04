package com.lin.elm.mapper;

import com.lin.elm.po.Seckill;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface SeckillMapper {

    /** 在售的秒杀活动（一条 join 把商品、商家带出来） */
    public List<Seckill> listOnSale();

    public Seckill getById(Integer seckillId);

    /**
     * 数据库扣库存：stock > 0 才会扣成功
     * 靠返回的"影响行数"判断是否抢到，天然防超卖（不需要 select ... for update）
     */
    @Update("update seckill set seckillStock = seckillStock - 1 where seckillId = #{seckillId} and seckillStock > 0")
    public int deductDbStock(@Param("seckillId") Integer seckillId);

    /** 异常补偿：把库存还回去 */
    @Update("update seckill set seckillStock = seckillStock + 1 where seckillId = #{seckillId}")
    public int restoreDbStock(@Param("seckillId") Integer seckillId);
}
