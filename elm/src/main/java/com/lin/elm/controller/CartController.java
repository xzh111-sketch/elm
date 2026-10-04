package com.lin.elm.controller;

import com.lin.elm.po.Cart;
import com.lin.elm.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("CartController")
public class CartController {
    @Autowired
    private CartService cartService;
    // 下面这些方法的 userId 一律以 token 为准，避免前端传别人的 userId 造成越权
    @RequestMapping("/listCart")
    public List<Cart> listCart(Cart cart, HttpServletRequest request){
        cart.setUserId((String) request.getAttribute("userId"));
        return cartService.listCart(cart);
    }
    @RequestMapping("/saveCart")
    public int saveCart(Cart cart, HttpServletRequest request){
        cart.setUserId((String) request.getAttribute("userId"));
        return cartService.saveCart(cart);
    }
    @RequestMapping("/updateCart")
    public int updateCart(Cart cart, HttpServletRequest request){
        cart.setUserId((String) request.getAttribute("userId"));
        return cartService.updateCart(cart);
    }
    @RequestMapping("/removeCart")
    public int removeCart(Cart cart, HttpServletRequest request){
        cart.setUserId((String) request.getAttribute("userId"));
        return cartService.removeCart(cart);
    }
}
