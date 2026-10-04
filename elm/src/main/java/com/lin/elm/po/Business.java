package com.lin.elm.po;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Business {
    private Integer businessId;
    private String businessName;
    private String businessImg;
    private String businessAddress;
    private String businessExplain;
    private Integer orderTypeId;
    private Double starPrice;
    private Double deliveryPrice;
    private String remarks;
}
