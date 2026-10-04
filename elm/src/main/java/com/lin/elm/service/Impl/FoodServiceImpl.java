package com.lin.elm.service.Impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lin.elm.conf.CacheKeys;
import com.lin.elm.mapper.FoodMapper;
import com.lin.elm.po.Food;
import com.lin.elm.service.FoodService;
import com.lin.elm.service.CacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FoodServiceImpl implements FoodService {
    @Autowired
    private FoodMapper foodMapper;
    @Autowired
    private CacheService cacheService;
    @Override
    public List<Food> listFoodByBusinessId(Integer businessId) {
        // 进商家详情页必查菜品列表，缓存起来
        return cacheService.getList(
                CacheKeys.foodList(businessId),
                new TypeReference<List<Food>>() {
                },
                () -> foodMapper.listFoodByBusinessId(businessId));
    }
    @Override
    public Food getFoodById(Integer foodId) {
        return cacheService.getObject(
                CacheKeys.foodInfo(foodId),
                Food.class,
                () -> foodMapper.getFoodById(foodId));
    }
}
