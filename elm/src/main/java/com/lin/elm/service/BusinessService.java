package com.lin.elm.service;


import com.lin.elm.po.Business;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface BusinessService {
    public List<Business> listBusinessByOrderTypeId(Integer orderTypeId);
    public Business getBusinessById(Integer businessId);
}
