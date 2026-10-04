package com.lin.elm.service;


import com.lin.elm.po.DeliveryAddress;


import java.util.List;

public interface DeliveryAddressService {
    public List<DeliveryAddress> listDeliveryAddressByUserId(String userId);
    public DeliveryAddress getDeliveryAddressById(Integer daId, String userId);
    public int saveDeliveryAddress(DeliveryAddress deliveryAddress);
    public int updateDeliveryAddress(DeliveryAddress deliveryAddress);
    public int removeDeliveryAddress(Integer daId, String userId);
}
