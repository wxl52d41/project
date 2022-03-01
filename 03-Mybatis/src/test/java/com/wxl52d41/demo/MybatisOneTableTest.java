package com.wxl52d41.demo;

import com.wxl52d41.demo.entity.User;
import com.wxl52d41.demo.mapper.UserMapper;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author xlwang55
 * @date 2022/2/28 11:19
 */
@SpringBootTest
public class MybatisOneTableTest {

    @Autowired
    private UserMapper mapper;

    @Test
    void findAll() {
        List<User> list = mapper.findAllUsers();
        for (User user : list) {
            System.out.println(user);
        }
    }


    //---------------------------查询: 多条件查询---------------------------

    /**
     * 根据id和username查询user表
     * 方案一: 参数列举出来
     *
     * @author xlwang55
     * @date 2022/2/28
     */
    @Test
    void findUsersByNameAndSex() {
        List<User> usersByNameAndSex = mapper.findUsersByNameAndSex("老王", "男");
        System.out.println("usersByNameAndSex = " + usersByNameAndSex);

    }

    /**
     * 方案二: 把这些参数都封装一个javabean中
     *
     * @author xlwang55
     * @date 2022/2/28
     */
    @Test
    void findUsersByNameAndSex2() {
        User user = new User();
        user.setUsername("老王");
        user.setSex("男");
        List<User> usersByNameAndSex = mapper.findUsersByNameAndSex2(user);
        System.out.println("usersByNameAndSex = " + usersByNameAndSex);

    }

    //---------------------------模糊查询---------------------------

    /**
     * 模糊查询
     *
     * @author xlwang55
     * @date 2022/2/28
     */
    @Test
    void findUsersBylike() {
        //方案一: 耦合度太高(java程序写sql语法)
        List<User> listUser1 = mapper.findUsersByKd1("%王%");
        List<User> listUser2 = mapper.findUsersByKd2("王");
        List<User> listUser3 = mapper.findUsersByKd3("王");
        //方案四: 正解
        List<User> listUser4 = mapper.findUsersByKd4("王");
        System.out.println("listUser1 = " + listUser1);
        System.out.println("listUser2 = " + listUser2);
        System.out.println("listUser3 = " + listUser3);
        System.out.println("listUser4 = " + listUser4);

    }


    @Test
    void addUser() {
        User user = new User();
        user.setUsername("大芳");
        user.setBirthday("1992-01-01");
        user.setAddress("北京");
        user.setSex("女");
        mapper.addUser(user);
        System.out.println("user主键:" + user.getId());

        User user2 = new User();
        user2.setUsername("小芳");
        user2.setBirthday("1992-01-01");
        user2.setAddress("北京");
        user2.setSex("女");
        mapper.addUser2(user2);
        System.out.println("user主键:" + user2.getId());
    }

    /**
     * # 动态sql  if 条件判断
     */
    @Test
    void findUsersByIdAndUserNameIf() {
        //select * from user where id = ? and username = ?
        List<User> list = mapper.findUsersByIdAndUserNameIf("41", "老王");

        //select * from user where id = ?
//        List<User> list = mapper.findUsersByIdAndUserNameIf("41", null);

        //select * FROM userWHERE username = ?
//        List<User> list = mapper.findUsersByIdAndUserNameIf(null, "老王");

        //select * FROM user;
//        List<User> list = mapper.findUsersByIdAndUserNameIf(null, null);

        for (User user : list) {
            System.out.println(user);
        }
    }

    /**
     * # 动态sql2 set 用于update语句
     */
    @Test
    public void updateUserById() {
        User user = new User();
        user.setUsername("小王");
        user.setSex("女");
        user.setId(41);
        //update user SET username = ?, sex = ? where id = ?
        mapper.updateUserById(user);
    }

    /**
     * 动态sql3 foreach 用于循环遍历
     * */
    @Test
    public void findUsersByIds() {

        //集合
        List<Integer> list = new ArrayList<>();
        Collections.addAll(list,41,42,45);
        List<User> result1 = mapper.findUsersByIds(list);

        //数组
        int[] array = {42, 43, 44};
        List<User> result = mapper.findUsersByIds2(array);
        for (User user : result) {
            System.out.println(user);
        }
    }

}
