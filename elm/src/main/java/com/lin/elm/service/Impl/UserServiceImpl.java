package com.lin.elm.service.Impl;

import com.lin.elm.mapper.UserMapper;
import com.lin.elm.po.User;
import com.lin.elm.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Override
    public User findByUserId(String userId) {
        return userMapper.findByUserId(userId);
    }
    @Override
    public int getUserById(String userId) {
        return userMapper.getUserById(userId);
    }
    @Override
    public int saveUser(User user) {
        return userMapper.saveUser(user);
    }

    @Override
    public int updateUser(User user) {
        return userMapper.updateUser(user);
    }

    @Override
    public int updateUserImg(User user) {
        return userMapper.updateUserImg(user);
    }

    @Override
    public int updatePassword(User user) {
        return userMapper.updatePassword(user);
    }

}
