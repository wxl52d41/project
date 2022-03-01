package com.wxl52d41.demo.entity;

import lombok.Data;

import java.util.List;

/**
 * @author xlwang55
 * @date 2022/2/28 11:04
 */
@Data
public class User {
    private Integer id;
    private String username;
    private String birthday;
    private String sex;
    private String address;

    private List<Orders> list; //一个用户可以拥有多个订单
}
