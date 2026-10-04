package com.lin.elm.mapper;

import com.lin.elm.po.OrderDetailet;


import java.util.List;

public interface OrderDetailetMapper {

    public int saveOrderDetailet(List<OrderDetailet> list);

    public List<OrderDetailet> listOrderDetailetByOrderId(Integer orderId);
}
