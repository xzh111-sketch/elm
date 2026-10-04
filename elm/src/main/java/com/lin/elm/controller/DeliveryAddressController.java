package com.lin.elm.controller;


import com.lin.elm.po.DeliveryAddress;
import com.lin.elm.service.DeliveryAddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("DeliveryAddressController")
public class DeliveryAddressController {
    @Autowired
    private DeliveryAddressService deliveryAddressService;
    // 下面所有方法的 userId 一律以 token 为准，防止查看/修改别人的收货地址
    @RequestMapping("/listDeliveryAddressByUserId")
    public List<DeliveryAddress> listDeliveryAddressByUserId(HttpServletRequest request){
        String userId = (String) request.getAttribute("userId");
        return deliveryAddressService.listDeliveryAddressByUserId(userId);
    }

    @RequestMapping("/getDeliveryAddressById")
    public DeliveryAddress getDeliveryAddressById(Integer daId, HttpServletRequest request){
        String userId = (String) request.getAttribute("userId");
        return deliveryAddressService.getDeliveryAddressById(daId, userId);
    }

    @RequestMapping("/saveDeliveryAddress")
    public int saveDeliveryAddress(DeliveryAddress deliveryAddress, HttpServletRequest request){
        deliveryAddress.setUserId((String) request.getAttribute("userId"));
        return deliveryAddressService.saveDeliveryAddress(deliveryAddress);
    }

    @RequestMapping("/updateDeliveryAddress")
    public int updateDeliveryAddress(DeliveryAddress deliveryAddress, HttpServletRequest request){
        deliveryAddress.setUserId((String) request.getAttribute("userId"));
        return deliveryAddressService.updateDeliveryAddress(deliveryAddress);
    }

    @RequestMapping("/removeDeliveryAddress")
    public int removeDeliveryAddress(Integer daId, HttpServletRequest request){
        String userId = (String) request.getAttribute("userId");
        return deliveryAddressService.removeDeliveryAddress(daId, userId);
    }
}
