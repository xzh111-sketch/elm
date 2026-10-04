package com.lin.elm.service;

import com.lin.elm.po.OrderDetailet;

import java.util.List;

public interface OrderDetailetService {
    public int saveOrderDetailet(List<OrderDetailet> list);
    public List<OrderDetailet> listOrderDetailetByOrderId(Integer orderId);
}
