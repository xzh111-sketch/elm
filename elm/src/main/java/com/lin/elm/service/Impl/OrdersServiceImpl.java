package com.lin.elm.service.Impl;

import com.lin.elm.conf.CommonUtil;
import com.lin.elm.mapper.CartMapper;
import com.lin.elm.mapper.OrderDetailetMapper;
import com.lin.elm.mapper.OrdersMapper;
import com.lin.elm.po.Cart;
import com.lin.elm.po.OrderDetailet;
import com.lin.elm.po.Orders;
import com.lin.elm.service.OrdersService;
import org.apache.ibatis.annotations.Select;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrdersServiceImpl implements OrdersService {
    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private OrderDetailetMapper orderDetailetMapper;
    @Autowired
    private CartMapper cartMapper;
    @Transactional
    public int saveOrders(Orders orders){
        //查询当前用户购物车中当前商家的所有食品
        Cart  cart = new Cart();
        cart.setUserId(orders.getUserId());
        cart.setBusinessId(orders.getBusinessId());
        List<Cart> carts = cartMapper.listCart(cart);
        //创建订单，并且返回生成订单编号
        orders.setOrderDate(CommonUtil.getCurrentDate());
        orders.setOrderState(0);
        ordersMapper.saveOrders(orders);
        Integer orderId = orders.getOrderId();

        //批量添加订单明细
        List<OrderDetailet> list  = new ArrayList<>();
        for ( Cart c : carts){
            OrderDetailet od = new OrderDetailet();
            od.setOrderId(orderId);
            od.setFoodId(c.getFoodId());
            od.setQuantity(c.getQuantity());
            list.add(od);
        }
        if (!list.isEmpty()) {
            orderDetailetMapper.saveOrderDetailet(list);
        }
        //删除购物车
        cartMapper.removeCart(cart);
        return orderId;
    }
    @Override
    public Orders getOrdersById(Integer orderId){
         return ordersMapper.getOrdersById(orderId);
    }
    @Override
    public List<Orders> listOrdersByUserId(String userId, Integer orderState, int offset, int pageSize){
         return ordersMapper.listOrdersByUserId(userId, orderState, offset, pageSize);
    }
    @Override
    public int countOrdersByUserId(String userId, Integer orderState){
         return ordersMapper.countOrdersByUserId(userId, orderState);
    }
    @Override
    public int updateOrderState(Integer orderId, Integer orderState){
        return ordersMapper.updateOrderState(orderId, orderState);
    }
    @Override
    public int updateTradeNo(Integer orderId, String tradeNo){
        return ordersMapper.updateTradeNo(orderId, tradeNo);
    }
}
