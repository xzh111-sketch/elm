package com.lin.elm.controller;


import com.lin.elm.po.Food;
import com.lin.elm.service.FoodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("FoodController")
public class FoodController {
    @Autowired
    private FoodService foodService;
    @RequestMapping("/listFoodByBusinessId")
    public List<Food> listFoodByBusinessId(Integer businessId){
        return foodService.listFoodByBusinessId(businessId);
    }
    @RequestMapping("/getFoodById")
    public Food getFoodById(Integer foodId){
        return foodService.getFoodById(foodId);
    }
}
