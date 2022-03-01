package com.wxl52d41.demo.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class Role implements Serializable {
    private Integer id;
    private String role_name;
    private String rolesc;

    private List<User> list; // 一个角色对应多个用户: 一对多
}