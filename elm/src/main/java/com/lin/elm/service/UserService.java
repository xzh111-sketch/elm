package com.lin.elm.service;

import com.lin.elm.po.User;

public interface UserService {
    public User findByUserId(String userId);
    public int getUserById(String userId);
    public int saveUser(User user);
    public int updateUser(User user);
    public int updateUserImg(User user);
    public int updatePassword(User user);
}
