package org.example.reggie.service;


import com.baomidou.mybatisplus.extension.service.IService;
import org.example.reggie.entity.Orders;

public interface OrdersService extends IService<Orders> {

    /**
     * 用户下单
     * @param orders
     */
    void submit(Orders orders);
}
