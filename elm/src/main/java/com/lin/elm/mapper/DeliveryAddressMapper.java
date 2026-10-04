package com.lin.elm.mapper;

import com.lin.elm.po.DeliveryAddress;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface DeliveryAddressMapper {
    @Select("select * from deliveryAddress where userId=#{userId} order by daId")
    public List<DeliveryAddress> listDeliveryAddressByUserId(String userId);
    // 带上 userId 条件：只能查到自己的地址
    @Select("select * from deliveryAddress where daId=#{daId} and userId=#{userId}")
    public DeliveryAddress getDeliveryAddressById(@Param("daId") Integer daId, @Param("userId") String userId);
    @Insert("insert into deliveryAddress values(NULL,#{contactName},#{contactSex},#{contactTel},#{address},#{userId})")
    public int saveDeliveryAddress(DeliveryAddress deliveryAddress);
    @Update("update deliveryAddress set contactName=#{contactName},contactSex=#{contactSex},contactTel=#{contactTel},address=#{address} where daId=#{daId} and userId=#{userId}")
    public int updateDeliveryAddress(DeliveryAddress deliveryAddress);
    @Delete("delete from deliveryAddress where daId=#{daId} and userId=#{userId}")
    public int removeDeliveryAddress(@Param("daId") Integer daId, @Param("userId") String userId);
}
