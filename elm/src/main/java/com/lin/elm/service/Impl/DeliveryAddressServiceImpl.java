package com.lin.elm.service.Impl;



import com.lin.elm.mapper.DeliveryAddressMapper;
import com.lin.elm.po.DeliveryAddress;
import com.lin.elm.service.DeliveryAddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeliveryAddressServiceImpl implements DeliveryAddressService {
    @Autowired
    private DeliveryAddressMapper deliveryAddressMapper;

    @Override
    public List<DeliveryAddress> listDeliveryAddressByUserId(String userId){
        return deliveryAddressMapper.listDeliveryAddressByUserId(userId);
    }
    @Override
    public DeliveryAddress getDeliveryAddressById(Integer daId, String userId){
        return deliveryAddressMapper.getDeliveryAddressById(daId, userId);
    }
    @Override
    public int saveDeliveryAddress(DeliveryAddress deliveryAddress){
        return deliveryAddressMapper.saveDeliveryAddress(deliveryAddress);
    }
    @Override
    public int updateDeliveryAddress(DeliveryAddress deliveryAddress){
        return deliveryAddressMapper.updateDeliveryAddress(deliveryAddress);
    }
    @Override
    public int removeDeliveryAddress(Integer daId, String userId){
        return deliveryAddressMapper.removeDeliveryAddress(daId, userId);
    }
}
