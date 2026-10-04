package com.lin.elm.mapper;

import com.lin.elm.po.Orders;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OrdersMapper {
    public int saveOrders(Orders orders);
    public Orders getOrdersById(Integer orderId);
    public List<Orders> listOrdersByUserId(@Param("userId") String userId,
                                           @Param("orderState") Integer orderState,
                                           @Param("offset") int offset,
                                           @Param("pageSize") int pageSize);
    public int countOrdersByUserId(@Param("userId") String userId, @Param("orderState") Integer orderState);
    public int updateOrderState(@Param("orderId") Integer orderId, @Param("orderState") Integer orderState);
    public int updateTradeNo(@Param("orderId") Integer orderId, @Param("tradeNo") String tradeNo);
}
