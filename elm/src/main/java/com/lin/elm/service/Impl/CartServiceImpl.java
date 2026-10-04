package com.lin.elm.service.Impl;

import com.lin.elm.mapper.CartMapper;
import com.lin.elm.po.Cart;
import com.lin.elm.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartMapper cartMapper;
    @Override
     public List<Cart> listCart(Cart cart) {
        return cartMapper.listCart(cart);
    }
    @Override
     public int saveCart(Cart cart) {
        return cartMapper.saveCart(cart);
    }
    @Override
    public int updateCart(Cart cart) {
        return cartMapper.updateCart(cart);
    }
    @Override
    public int removeCart(Cart cart) {
        return cartMapper.removeCart(cart);
    }
}
