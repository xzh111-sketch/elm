package com.lin.elm.service.Impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.lin.elm.conf.CacheKeys;
import com.lin.elm.mapper.BusinessMapper;
import com.lin.elm.po.Business;
import com.lin.elm.service.BusinessService;
import com.lin.elm.service.CacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BusinessServiceImpl implements BusinessService {
    @Autowired
    private BusinessMapper businessMapper;
    @Autowired
    private CacheService cacheService;

    @Override
    public List<Business> listBusinessByOrderTypeId(Integer orderTypeId) {
        // 商家列表是典型的热点数据（首页/分类页每次都要查），加缓存
        // 穿透、击穿、雪崩的防护都封装在 CacheService 里，这里业务代码保持干净
        return cacheService.getList(
                CacheKeys.businessList(orderTypeId),
                new TypeReference<List<Business>>() {
                },
                () -> businessMapper.listBusinessByOrderTypeId(orderTypeId));
    }

    @Override
    public Business getBusinessById(Integer businessId) {
        return cacheService.getObject(
                CacheKeys.businessInfo(businessId),
                Business.class,
                () -> businessMapper.getBusinessById(businessId));
    }
}
