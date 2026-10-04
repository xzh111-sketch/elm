package com.lin.elm.po;

import lombok.Data;

import java.util.List;

@Data
public class Orders {
    private Integer orderId;
    private String userId;
    private Integer businessId;
    private String orderDate;
    private Double orderTotal;
    private Integer daId;
    private Integer orderState;
    private String tradeNo;
    private Business business;
    private List<OrderDetailet> orderDetailet;
}
