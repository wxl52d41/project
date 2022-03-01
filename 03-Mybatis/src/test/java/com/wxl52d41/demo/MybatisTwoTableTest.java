package com.wxl52d41.demo;

import com.wxl52d41.demo.entity.Orders;
import com.wxl52d41.demo.entity.Role;
import com.wxl52d41.demo.entity.User;
import com.wxl52d41.demo.mapper.OrdersMapper;
import com.wxl52d41.demo.mapper.RoleMapper;
import com.wxl52d41.demo.mapper.UserMapper;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author xlwang55
 * @date 2022/2/28 16:42
 */
@SpringBootTest
public class MybatisTwoTableTest {
    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;

    //--------------------多表连接查询------------------------------

    /**
     * 一对一
     * 查询id=?的订单以及对应的用户
     */
    @Test
    public void findOrderByIdWithUser() {
        // 订单编号 = 1
        Orders orders = ordersMapper.findOrderByIdWithUser(1);
        System.out.println("订单:" + orders);
        Orders orders2 = ordersMapper.findOrderByIdWithUser2(1);
        System.out.println("订单2:" + orders2);
    }


    /**
     * 一对多
     * 查询id=? 的用户以及拥有的订单
     */
    @Test
    public void findUserByIdWithOrders() {
        // 查询用户id=41的用户以及拥有的订单
        User user = userMapper.findUserByIdWithOrders(41);
        System.out.println(user);
    }

    /**
     * 一对多
     * 查询id=? 的用户以及拥有的订单
     */
    @Test
    public void findRoleByIdWithUsers() {
        //查询角色id=1的角色以及对应的用户
        Role role = roleMapper.findRoleByIdWithUsers(1);
        System.out.println("role = " + role);

    }


    //--------------------级联查询------------------------------

    /**
     * 一对一
     * 查询id=?的订单以及对应的用户
     */
    @Test
    public void findOrderByIdWithUser3() {
        // 订单编号 = 1
        Orders orders = ordersMapper.findOrderByIdWithUser3(1);
        System.out.println("订单:" + orders);
    }


    /**
     * 一对多
     * 查询id=? 的用户以及拥有的订单
     */
    @Test
    public void findUserByIdWithOrders2() {
        // 查询用户id=41的用户以及拥有的订单
        User user = userMapper.findUserByIdWithOrders2(41);
        System.out.println(user);
    }

}
