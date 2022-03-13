package com.wxl52d41.mybatis_plus.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wxl52d41.mybatis_plus.entity.User;
import com.wxl52d41.mybatis_plus.mapper.UserMapper;
import com.wxl52d41.mybatis_plus.service.UserService;
import org.springframework.stereotype.Service;

/*** ServiceImpl实现了IService，提供了IService中基础功能的实现 * 若ServiceImpl无法满足业务需求，则可以使用自定的UserService定义方法，并在实现类中实现 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}