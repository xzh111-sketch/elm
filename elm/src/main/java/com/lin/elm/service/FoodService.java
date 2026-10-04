package com.lin.elm.service;

import com.lin.elm.po.Food;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface FoodService {

    public List<Food> listFoodByBusinessId(Integer businessId);

    public Food getFoodById(Integer foodId);
}
