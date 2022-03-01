package com.wxl52d41.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wxl52d41.demo.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author xlwang55
 * @date 2022/2/28 11:03
 */
public interface UserMapper extends BaseMapper<User> {


    /**
     * # 查询 : 查询表中所有的用户
     * 1.sql  select id,username,birthday,sex,address from user
     * 2.没有参数
     * 3.返回值类型: List<User>
     */
    List<User> findAllUsers();


    //--------------查询: 多条件查询-----------

    /**
     * 方案一: 参数列举出来
     * 1. sql  select *  from user where username = ? and sex = ?
     * 2. 参数  String , String
     * 3. 返回值类型: List<User>
     * <p>
     * 接口和映射文件(参数映射)
     * 1. 如果只有一个参数,可以不用写参数映射
     * 2. 如果有2个以及以上,必须要写参数映射  -> BindException
     *
     * @Param("name") String name  : 相当于告诉mybatis映射文件,这个属性是name
     */
    List<User> findUsersByNameAndSex(@Param("name") String name, @Param("sex") String sex);

    /**
     * 方案二: 把这些参数都封装一个javabean中
     */
    List<User> findUsersByNameAndSex2(User user);


    //--------------模糊查询-----------

    /**
     * # 模糊查询
     * sql : select * from user where username like ?
     * 参数 : keyword = 王  , 拼接之后: %王%
     * 返回值:  List<User>
     */

    List<User> findUsersByKd1(String keyword);//select * FROM user WHERE username like '%王%';

    List<User> findUsersByKd2(String keyword);//select * FROM user WHERE username like '%' '王' '%';

    List<User> findUsersByKd3(String keyword);//select * FROM user WHERE username like '%王%';

    List<User> findUsersByKd4(String keyword);//select * FROM user WHERE username like concat('%','王','%');


    /**
     * # 复杂操作:插入一条数据返回对应的主键
     * sql: insert into user values(null,?,?,?,?);
     * 参数: user(username,birthday,sex,address)
     * 返回值: int(其实是被影响的行数)
     * void
     * <p>
     * 对应的主键: user.id
     */
    void addUser(@Param("user") User user);

    void addUser2(User user);


    //-------------------------动态sql------------------------
    /**
     * # 动态sql
     *   需求: 查询符合一定条件的用户(条件是可选的)
     *       select * from user
     *           where
     *               id = ?      -- 可能有
     *           and username = ?  -- 可能有
     *
     *   参数: int id, String username
     *   返回值: List<U>
     * */
    List<User> findUsersByIdAndUserNameIf(@Param("id") String id, @Param("username") String username);


    /**
     * # 动态sql2  set 用于update语句
     *   需求: 修改某个用户信息
     *       update user
     *           set username = ?,   -- 动态
     *               birthday = ?,    -- 动态
     *               sex = ?,         -- 动态
     *               address = ?      -- 动态
     *          where id = ?;
     *   参数: User
     *   返回值: void
     *
     * */
    void updateUserById(User user);


    /**
     * # 动态sql3 foreach 用于循环遍历
     *   需求: 查询某几个用户信息
     *       select * from user where id in
     *            (41,43,46)   -> 动态的
     *
     *   参数: List<Integer> list / int[] array
     *   返回值: List<User>
     * */
    List<User> findUsersByIds(List<Integer> list);

    List<User> findUsersByIds2(int[] array);


    //------------------------多表连接查询---------------------
    /**
     * # 查询id=? 的用户以及拥有的订单
        select * from user u inner join orders o
            on u.id = o.uid
        where u.id = ?;
     *   1. 参数类型: Integer
     *   2. 返回值类型: User
     * */
    User findUserByIdWithOrders(Integer uid);


    //------------------------级联查询（）----------------------

    /**
     * # 查询id=? 的用户以及拥有的订单
       select * from user u inner join orders o
         on u.id = o.uid
       where u.id = ?;
     *   1. 参数类型: Integer
     *   2. 返回值类型: User
     * # 修改成嵌套查询
     *       a. 查询用户id=? 的用户
     *           select * from user where user.id = ?;
     *
     *       b. 查询该用户的订单信息
     *           select * from orders where uid = ?;
     *
     *       c. 嵌套
     * */
    User findUserByIdWithOrders2(Integer uid);


    /**
     *  select * from user where id = ?
     *  1. 参数: Integer id 用户标号
     *  2. 返回值: User
     * */
    User findUserById(Integer id);

}
