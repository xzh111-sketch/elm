package com.lin.elm.service;

import com.lin.elm.po.Orders;

import java.util.List;

public interface OrdersService {
    public int saveOrders(Orders orders);
    public Orders getOrdersById(Integer orderId);
    public List<Orders> listOrdersByUserId(String userId, Integer orderState, int offset, int pageSize);
    public int countOrdersByUserId(String userId, Integer orderState);
    public int updateOrderState(Integer orderId, Integer orderState);
    public int updateTradeNo(Integer orderId, String tradeNo);
}
