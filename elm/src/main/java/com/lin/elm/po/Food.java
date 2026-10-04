package com.lin.elm.po;

import lombok.Data;


@Data

public class Food {
    public Integer foodId;
    public String foodName;
    public String foodExplain;
    public String foodImg;
    public Double foodPrice;
    public Integer businessId;
    public String remarks;
}

