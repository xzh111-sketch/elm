package com.lin.elm.mapper;

import com.lin.elm.po.SeckillOrder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface SeckillOrderMapper {

    /** 新增参与记录；表上的唯一索引 (seckillId, userId) 会拦住重复插入 */
    @Insert("insert into seckill_order(seckillId, userId, seckillPrice, createTime, status) "
            + "values(#{seckillId}, #{userId}, #{seckillPrice}, #{createTime}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "soId")
    public int insert(SeckillOrder seckillOrder);

    @Select("select * from seckill_order where seckillId = #{seckillId} and userId = #{userId}")
    public SeckillOrder getByUserAndSeckill(@Param("seckillId") Integer seckillId,
                                            @Param("userId") String userId);

    @Update("update seckill_order set orderId = #{orderId}, status = 1 where soId = #{soId}")
    public int markSuccess(@Param("soId") Long soId, @Param("orderId") Integer orderId);

    @Update("update seckill_order set status = 2 where soId = #{soId}")
    public int markFailed(@Param("soId") Long soId);
}
