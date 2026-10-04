package com.lin.elm.service.Impl;



import com.lin.elm.mapper.OrderDetailetMapper;


import com.lin.elm.po.OrderDetailet;
import com.lin.elm.service.OrderDetailetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderDetailetServiceImpl implements OrderDetailetService {
    @Autowired
    private OrderDetailetMapper orderDetailetMapper;
    @Override
    public int saveOrderDetailet(List<OrderDetailet> list){

        return orderDetailetMapper.saveOrderDetailet(list);
    }
    @Override
    public List<OrderDetailet> listOrderDetailetByOrderId(Integer orderId) {
        return orderDetailetMapper.listOrderDetailetByOrderId(orderId);
    }

}
