package com.lin.elm.po;

import lombok.Data;

@Data
public class User {
    private String userId;
    private String userName;
    private String password;
    private Integer userSex;
    private String userImg;
    private Integer delTag;
}
