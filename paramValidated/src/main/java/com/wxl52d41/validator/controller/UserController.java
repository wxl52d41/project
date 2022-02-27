package com.wxl52d41.validator.controller;

import com.wxl52d41.validator.validator.InsertValidator;
import com.wxl52d41.validator.validator.UpdateValidator;
import com.wxl52d41.validator.vo.User;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author wxl
 * @version 1.0.0
 * @ClassName UserController.java
 * @Description TODO
 * @createTime 2022年02月26日 21:14:00
 */
@RestController
@RequestMapping("/user")
public class UserController {
    /**
     * 注册用户
     *
     * @param user 用户详细信息
     * @return
     */
    @PostMapping(path = "/insertUser")
    public void insertUser(@Validated(InsertValidator.class) @RequestBody User user) {
        System.out.println("user = " + user);
    }

    /**
     * 编辑用户详细信息
     *
     * @param user 用户详细信息
     * @return
     */
    @PostMapping(path = "/updateUser")
    public void updateUser(@Validated(UpdateValidator.class) @RequestBody User user) {
        System.out.println("user = " + user);
    }

    /**
     * 修改用户密码
     *
     * @param password    原密码
     * @param newPassword 新密码
     * @param id          用户ID
     * @return
     */
    @PutMapping(path = "/updatePassword")
    public void updatePassword(@RequestParam(name = "password", required = true) String password,
                               @RequestParam(name = "newPassword", required = true) String newPassword,
                               @RequestParam(name = "id", required = true) Long id) {

    }

}
