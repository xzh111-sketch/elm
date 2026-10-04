package com.lin.elm.controller;

import com.lin.elm.po.OrderDetailet;
import com.lin.elm.service.OrderDetailetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("OrderDetailetController")
public class OrderDetailetController {
    @Autowired
    private OrderDetailetService orderDetailetService;
    
    @PostMapping(value = "/saveOrderDetailet")
    public int saveOrderDetailet(@RequestBody List<OrderDetailet> list){
        return orderDetailetService.saveOrderDetailet(list);
    }
    
    @RequestMapping("/listOrderDetailetByOrderId")
    public List<OrderDetailet> listOrderDetailetByOrderId(Integer orderId){
        return orderDetailetService.listOrderDetailetByOrderId(orderId);
    }
}
