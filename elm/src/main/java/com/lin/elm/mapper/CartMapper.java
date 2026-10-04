package com.lin.elm.mapper;

import com.lin.elm.po.Cart;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface CartMapper {

    public List<Cart> listCart(Cart cart);
    // 依赖 cart 表的唯一约束 uk_cart_user_food(userId, foodId)：
    // 已有该商品就数量 +1，没有才插入新行，从根上避免购物车出现重复行
    @Insert("insert into cart(foodId, businessId, userId, quantity) "
            + "values(#{foodId}, #{businessId}, #{userId}, 1) "
            + "on duplicate key update quantity = quantity + 1")
    public int saveCart(Cart cart);
    @Update("update cart set quantity=#{quantity} where foodId=#{foodId} and businessId=#{businessId} and userId=#{userId}")
    public int updateCart(Cart cart);

    public int removeCart(Cart cart);
}
