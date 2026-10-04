package com.lin.elm.mapper;

import com.lin.elm.po.Business;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface BusinessMapper {

    @Select("select * from business where orderTypeId=#{orderTypeId} order by businessId")
    public List<Business> listBusinessByOrderTypeId(Integer orderTypeId);

    @Select("select * from business where businessId=#{businessId}")
    public Business getBusinessById(Integer businessId);
}
