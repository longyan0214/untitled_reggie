package org.example.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.reggie.common.BaseContext;
import org.example.reggie.common.R;
import org.example.reggie.entity.OrderDetail;
import org.example.reggie.entity.Orders;
import org.example.reggie.entity.ShoppingCart;
import org.example.reggie.service.OrderDetailService;
import org.example.reggie.service.OrdersService;
import org.example.reggie.service.ShoppingCartService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private OrderDetailService orderDetailService;

    /**
     * 用户下单
     *
     * @param orders
     * @return
     */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders) {
        ordersService.submit(orders);
        return R.success("下单成功！");
    }

    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, Long number, String beginTime, String endTime) {
        /*
        log.info("beginTime:{}",beginTime);
        log.info("endTime:{}",endTime);
        */

        Page pageInfo = new Page(page, pageSize);

        // 查询所有orders表信息
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        // 查询name
        if (number != null) {
            queryWrapper.like(Orders::getNumber, number);
        }
        // 查询beginTime 大于等于这个时间
        if (beginTime != null) {
            queryWrapper.ge(Orders::getOrderTime, beginTime);
        }
        // 查询endTime 小于等于这个时间
        if (endTime != null) {
            queryWrapper.le(Orders::getOrderTime, endTime);
        }
        queryWrapper.orderByDesc(Orders::getOrderTime);
        ordersService.page(pageInfo, queryWrapper);
        return R.success(pageInfo);
    }

    /**
     * 订单状态修改——管理端
     *
     * @param orders
     * @return
     */
    @PutMapping
    public R<String> order(@RequestBody Orders orders) {
        // log.info("orders:{}", orders);
        Orders order = ordersService.getById(orders.getId());
        if (order.getStatus() == 2) {
            orders.setStatus(3);
            ordersService.updateById(orders);
            return R.success("订单派送成功");
        } else {
            orders.setStatus(4);
            ordersService.updateById(orders);
            return R.success("订单已完成");
        }
    }

    @GetMapping("/userPage")
    public R<Page> userPage(int page, int pageSize, HttpSession session) {
        Page<Orders> pageInfo = new Page(page, pageSize);

        // 查询所有订单
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        // 查询该用户的所有订单信息
        //queryWrapper.eq(Orders::getUserId, session.getAttribute("user"));
        queryWrapper.eq(Orders::getUserId, BaseContext.getCurrentId());

        queryWrapper.orderByDesc(Orders::getOrderTime);
        ordersService.page(pageInfo, queryWrapper);
        return R.success(pageInfo);
    }

    /**
     * 再来一单
     * @param orders
     * @param session
     * @return
     */
    @PostMapping("/again")
    public R<String> again(@RequestBody Orders orders, HttpSession session){
        //log.info("orders:{}", orders);//orders:Orders(id=1565321137458069506, number=null, status=null, userId=null, addressBookId=null, orderTime=null, checkoutTime=null, payMethod=null, amount=null, remark=null, phone=null, address=null, userName=null, consignee=null)
        //1.根据订单id查询详细订单信息
        LambdaQueryWrapper<OrderDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderDetail::getOrderId, orders.getId());
        List<OrderDetail> list = orderDetailService.list(queryWrapper);
        //2.以流的形式添加每一份餐品到购物车
        List<ShoppingCart> shoppingCartList = list.stream().map((item) -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            //赋值orderDetail到shoppingCart
            BeanUtils.copyProperties(item, shoppingCart);
            //log.info("shoppingCart:{}", shoppingCart);//shoppingCart:ShoppingCart(id=1568512162989137922, name=口味蛇, image=0f4bd884-dc9c-4cf9-b59e-7d5958fec3dd.jpg, userId=null, dishId=1397851668262465537, setmealId=null, dishFlavor=少冰, number=1, amount=168.00, createTime=null)
            //设置userId
            shoppingCart.setUserId((Long) session.getAttribute("user"));
            //设置createTime
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());
        //log.info("shoppingCartList:{}", shoppingCartList);

        //先清空购物车  根据用户id
        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingCart::getUserId, (Long) session.getAttribute("user"));
        shoppingCartService.remove(wrapper);
        //批量保存到购物车
        shoppingCartService.saveBatch(shoppingCartList);

        return R.success("");
    }

}
