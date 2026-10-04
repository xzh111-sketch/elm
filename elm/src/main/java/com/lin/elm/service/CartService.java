package com.lin.elm.service;

import com.lin.elm.po.Cart;

import java.util.List;

public interface CartService {
    public List<Cart> listCart(Cart cart);

    public int saveCart(Cart cart);

    public int updateCart(Cart cart);

    public int removeCart(Cart cart);

}
