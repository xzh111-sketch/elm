package com.lin.elm.po;

import lombok.Data;


import java.util.List;

@Data

public class OrderDetailet {
    private Integer odId;
    private Integer orderId;
    private Integer foodId;
    private Integer quantity;
    public Food food;
    public List<OrderDetailet> orderDetailetList;
}
