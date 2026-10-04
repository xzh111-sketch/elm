package com.lin.elm.mapper;

import com.lin.elm.po.Food;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface FoodMapper {
    @Select("select * from food where businessId=#{businessId}")
    public List<Food> listFoodByBusinessId(Integer businessId);
    @Select("select * from food where foodId=#{foodId}")
    public Food getFoodById(Integer foodId);
}
