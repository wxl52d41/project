package com.wxl52d41.mybatis_plus.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wxl52d41.mybatis_plus.enums.SexEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: User
 * @Description: User
 * @Author: wang xiao le
 * @Date: 2022/3/12 21:00
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
//@TableName("t_user")
public class User {

    @TableId
    private Long id;
    private String name;
    private Integer age;
    private String email;

    @TableLogic
    private  Integer isDeleted;


    private SexEnum sex;

    public User(Long id, String name, Integer age, String email, Integer isDeleted) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.email = email;
        this.isDeleted = isDeleted;
    }
}
