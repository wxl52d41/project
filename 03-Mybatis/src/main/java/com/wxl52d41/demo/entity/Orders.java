package com.wxl52d41.demo.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class Orders implements Serializable {
    private Integer id;
    private String ordertime;
    private Double money;

    private Integer uid; //外键

    private User user; // 一个订单属于一个用户
} 