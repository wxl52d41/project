package com.wxl52d41.validator.vo;

import com.wxl52d41.validator.validator.InsertValidator;
import com.wxl52d41.validator.validator.TelephoneNumber;
import com.wxl52d41.validator.validator.UpdateValidator;
import lombok.Data;

import javax.validation.constraints.*;

/**
 * @author wxl
 * @version 1.0.0
 * @ClassName User.java
 * @Description TODO
 * @createTime 2022年02月26日 20:58:00
 */
@Data
public class User {


    @NotNull(groups = {UpdateValidator.class})
    private Long id;// ID
    /**
     * 新增时校验条件生效
     * 用户名不能为空，且用户名个数在1-20之间。
     */
    @NotEmpty(message = "username必填", groups = {InsertValidator.class})
    @Size(max = 20, min = 1, message = "用户名个数在1-20之间", groups = {InsertValidator.class})
    private String username;// 用户名

    @NotBlank(message = "密码必填", groups = {InsertValidator.class})
    @Size(min = 6, message = "密码个数不少于六位", groups = {InsertValidator.class})
    private String password;// 密码

    /**
     * 新增时校验条件生效
     * 邮箱校验规则
     */
    @NotBlank(message = "邮箱不能为空", groups = {InsertValidator.class})
    @Email(groups = {InsertValidator.class})
    @Size(max = 30, message = "邮箱个数不多于30位", groups = {InsertValidator.class})
    private String email;// 邮箱

    /**
     * 新增更新时校验条件生效
     * 使用了手机号校验规则
     */
    @NotBlank(message = "手机号不能为空", groups = {InsertValidator.class})
    @TelephoneNumber(groups = {InsertValidator.class, UpdateValidator.class}, message = "手机号不正确")
    @Size(max = 11, groups = {InsertValidator.class, UpdateValidator.class})
    private String mobile;// 手机号

    @Size(max = 20, min = 6, groups = {InsertValidator.class})
    private String provinceRegionCode;// 省份区域代码

    @Size(max = 20, min = 6, groups = {UpdateValidator.class})
    private String cityRegionCode;// 地市区域代码

    @Size(max = 40, groups = {UpdateValidator.class})
    private String address;// 街道地址

    @Size(max = 256, groups = {UpdateValidator.class})
    private String profile;// 个人简介

}
