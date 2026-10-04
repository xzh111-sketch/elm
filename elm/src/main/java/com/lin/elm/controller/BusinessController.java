package com.lin.elm.controller;

import com.lin.elm.po.Business;
import com.lin.elm.service.BusinessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("BusinessController")
public class BusinessController {
    @Autowired
    private BusinessService businessService;

    @RequestMapping("/listBusinessByOrderTypeId")
    public List<Business> listBusinessByOrderTypeId(Integer orderTypeId){
        return businessService.listBusinessByOrderTypeId(orderTypeId);
    }

    @RequestMapping("/getBusinessById")
    public Business getBusinessById(Integer businessId){
        return businessService.getBusinessById(businessId);
    }
}
