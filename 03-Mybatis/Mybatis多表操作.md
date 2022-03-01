# 一 MyBatis多表查询

**数据准备**

```sql

/*Table structure for table `user` */
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  `id` int(11) NOT NULL auto_increment,
  `username` varchar(32) NOT NULL COMMENT '用户名称',
  `birthday` datetime default NULL COMMENT '生日',
  `sex` varchar(10) default NULL COMMENT '性别',
  `address` varchar(256) default NULL COMMENT '地址',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

/*Data for the table `user` */

insert  into `user`(`id`,`username`,`birthday`,`sex`,`address`) values (41,'老王','2019-05-27 17:47:08','男','北京'),(42,'王小二','2019-03-02 15:09:37','女','北京金燕龙'),(43,'老李','2019-03-04 11:34:34','女','北京修正'),(45,'北京','2019-03-04 12:04:06','男','北京金燕龙'),(46,'王小二','2018-09-07 17:37:26','男','北京TBD'),(48,'小马宝莉','2019-03-08 11:44:00','女','北京修正');

DROP TABLE IF EXISTS `orders`;

CREATE TABLE `orders` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uid` int(11) DEFAULT NULL,
  `ordertime` datetime DEFAULT NULL,
  `money` double DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `uid` (`uid`),
  CONSTRAINT `orders_ibfk_1` FOREIGN KEY (`uid`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;

/*Data for the table `orders` */

insert  into `orders`(`id`,`uid`,`ordertime`,`money`) values (1,41,'2019-05-20 02:58:02',999.5),(2,45,'2019-02-14 07:58:00',1399),(3,41,'2019-06-01 21:00:02',1666);

DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
  `id` int(11) NOT NULL COMMENT '编号',
  `role_name` varchar(30) default NULL COMMENT '角色名称',
  `role_desc` varchar(60) default NULL COMMENT '角色描述',
  PRIMARY KEY  (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

/*Data for the table `role` */
insert  into `role`(`ID`,`role_name`,`role_desc`) values (1,'院长','管理整个学院'),(2,'总裁','管理整个公司'),(3,'校长','管理整个学校');


/*Table structure for table `user_role` */
DROP TABLE IF EXISTS `user_role`;

CREATE TABLE `user_role` (
  `uid` int(11) NOT NULL COMMENT '用户编号',
  `rid` int(11) NOT NULL COMMENT '角色编号',
  PRIMARY KEY  (`uid`,`rid`),
  KEY `FK_Reference_10` (`rid`),
  CONSTRAINT `FK_Reference_10` FOREIGN KEY (`rid`) REFERENCES `role` (`id`),
  CONSTRAINT `FK_Reference_9` FOREIGN KEY (`uid`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

/*Data for the table `user_role` */

insert  into `user_role`(`uid`,`rid`) values (41,1),(45,1),(41,2);


```
![在这里插入图片描述](https://img-blog.csdnimg.cn/9ed721be178b4befb8a286abc63aec57.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBA5LyP5Yqg54m56YGH5LiK6KW_5p-a,size_20,color_FFFFFF,t_70,g_se,x_16)
```markdown
# 多表关系
1. sql中
	a. 一对一
	b. 一对多 : 用户 和 订单
	c. 多对多 : 用户 和 角色

2. mybatis中
	a. 一对一
	b. 一对多

```



```java
# user表和orders表: 一对多
public class User implements Serializable {
    private Integer id;
    private String username;
    private String birthday;
    private String sex;
    private String address;
    
    private List<Orders> list; //一个用户可以拥有多个订单
}

public class Orders implements Serializable {
    private Integer id;
    private String ordertime;
    private Double money;

    private Integer uid; //外键
    private User user; // 一个订单属于一个用户
}   

# 1. mybatis看问题的角度不同
// a. 从user表的角度看问题 : 一对多(一个用户有多个订单)
//b. 从orders表的角度看问题 : 一对一(一个订单只能属于一个用户)

# 2. mybatis解决问题的方式不同
// a. sql中是用外键建立表关系
// b. mybatis中用属性

```



## 1.1 一对一

**一对一查询模型**

用户表和订单表的关系为，一个用户有多个订单，一个订单只从属于一个用户

一对一查询的需求：查询一个订单，与此同时查询出该订单所属的用户
![在这里插入图片描述](https://img-blog.csdnimg.cn/93406d7cb5e541ad897932b6fa2ad7c9.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBA5LyP5Yqg54m56YGH5LiK6KW_5p-a,size_20,color_FFFFFF,t_70,g_se,x_16)


#### ① 实体和表映射关系

```sql
# 查询id=1的订单以及对应的用户
select * from orders o inner join user u
	on o.uid = u.id
		where o.id = 1;
```

![在这里插入图片描述](https://img-blog.csdnimg.cn/5076e076ae3a4b90a9989a4460fee762.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBA5LyP5Yqg54m56YGH5LiK6KW_5p-a,size_20,color_FFFFFF,t_70,g_se,x_16)
#### ② Order实体类
```java
public class Orders implements Serializable {
    private Integer id;
    private String ordertime;
    private Double money;

    private Integer uid; //外键
    private User user; // 订单对用户: 一对一
}
```
#### ③ OrderMapper接口
```java
   /**
     * # 查询id=?的订单以及对应的用户
     * select * from orders o inner join user u
     * on o.uid = u.id
     * where o.id = ?;
     * <p>
     * 1. 参数类型: Integer id
     * 2. 返回值类型  Orders
     */
    Orders findOrderByIdWithUser(Integer id);  //xml返回用resultMap

    /**
     * 直接用orders作为返回对象，user会映射不出来导致结果不符合预期
     * 正确使用：将user中的属性复制到orders中即可
     */
    Orders findOrderByIdWithUser2(Integer id);   //xml返回用resultType,
}
```



#### ④ OrdersMapper.xml
```xml
  <!--
        autoMapping = true
        自动映射: 当javabean的属性名和结果集的字段名一致, 自动映射
    -->
    <resultMap id="myorder" type="orders" autoMapping="true">
        <!--
            id标签 : 主键字段映射(必须要写)
            result标签: 非主键字段映射 (同名可不写,推荐写)
        -->
        <!--最左原则: 如果结果集中存在同名字段,默认使用左边的那个-->
        <id property="id" column="id"/>
        <!-- 写一个意思一下,其他两个字段不写 -->
        <result property="ordertime" column="ordertime"/>
        <!--
            重点在: orders.user 属性 跟 结果集某一段(user表查出来)的映射
                1. 一对一: association  (使用)
                2. 一对多: collection

            # association 标签
                1. properties : 属性名
                2. javaType : 此属性的类型

               子标签 : 设置user和结果集之间的映射
                   注意 最左原则
        -->
        <association property="user" javaType="user" autoMapping="true">
            <id property="id" column="uid"/>
            <!--写一个意思一下-->
            <result property="username" column="username"/>
        </association>
    </resultMap>
    <!--
        1. 直接写resultType=orders, 返回类型=orders
        2. 要写成 resultMap : 结果集映射
             指定查询结果集 跟 javabean (orders) 之间的映射关系
    -->
    <select id="findOrderByIdWithUser" resultMap="myorder">
         select u.*,o.*
         from orders o inner join user u on o.uid = u.id
         where o.id = #{id}
    </select>

    <!--1. 直接写resultType=orders, 返回类型=orders
            直接用orders作为返回对象，user会映射不出来导致结果不符合预期-->
    <select id="findOrderByIdWithUser2" resultType="orders">
         select u.*,o.*
         from orders o inner join user uon o.uid = u.id
         where o.id = #{id}
    </select>

```



#### ⑤ 测试
```java
    /**
     * 查询id=?的订单以及对应的用户
     */
    @Test
    public void method01() {
        // 订单编号 = 1
        Orders orders = mapper.findOrderByIdWithUser(1);
        System.out.println("订单:" + orders);
        Orders orders2 = mapper.findOrderByIdWithUser2(1);
        System.out.println("订单2:" + orders2);

    }
```
## 1.2 一对多

**一对多查询模型**

用户表和订单表的关系为，一个用户有多个订单，一个订单只从属于一个用户

一对多查询的需求：查询一个用户，与此同时查询出该用户具有的订单
![在这里插入图片描述](https://img-blog.csdnimg.cn/b8bf37a818df4aee94b505ed489b997d.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBA5LyP5Yqg54m56YGH5LiK6KW_5p-a,size_20,color_FFFFFF,t_70,g_se,x_16)
#### ① 实体和表关系

```sql
SELECT *,o.id AS oid FROM `user` u INNER JOIN orders o ON u.`id` = o.`uid` WHERE u.`id`=41
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/2059d601733a435b815f4d20520aefd6.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBA5LyP5Yqg54m56YGH5LiK6KW_5p-a,size_20,color_FFFFFF,t_70,g_se,x_16)
#### ② User实体类

```java
public class User {

    private Integer id;
    private String username;
    private String birthday;
    private String sex;
    private String address;

    // 一个用户具有多个订单
    private List<Order> list;
}
```

#### ③ UserMapper接口

```java
    /**
     * # 查询id=? 的用户以及拥有的订单
        select * from user u inner join orders o
            on u.id = o.uid
        where u.id = ?;
     *   1. 参数类型: Integer
     *   2. 返回值类型: User
     * */
    User findUserByIdWithOrders(Integer uid);
```
#### ④ UserMapper.xml
![在这里插入图片描述](https://img-blog.csdnimg.cn/aeb89bb51c834c3f9a0c1e92074fcf1c.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBA5LyP5Yqg54m56YGH5LiK6KW_5p-a,size_20,color_FFFFFF,t_70,g_se,x_16)


```xml
<!--- ************************ 一对多 ***********************************-->
  <resultMap id="myuser" type="user" autoMapping="true">
        <id property="id" column="id"/>
        <!--
            重点: user.list<Orders> list 属性 跟 结果集的一部分进行映射

            关系: 一个用户对应多个订单 (一对多)
                1. association : 一对一
                2. collection : 一对多

            # collection 标签
                1. property : 指定属性名
                2. ofType : 指定集合的元素类型

             注意: 结果集同名字段的最左原则
        -->
        <collection property="list" ofType="orders" autoMapping="true">
            <id property="id" column="oid"/>
        </collection>
    </resultMap>

    <select id="findUserByIdWithOrders" resultMap="myuser">
         select u.*,o.id as oid,o.uid,o.ordertime,o.money
         from user u inner join orders o on u.id = o.uid
         where u.id = #{uid}
    </select>
```
#### ⑤ 测试
```java
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
```

## 1.3 多对多（由二个一对多组成）

**多对多查询的模型**

用户表和角色表的关系为，一个用户有多个角色，一个角色被多个用户使用

多对多查询的需求：查询用户同时查询出该用户的所有角色

**在mybatis中多对多实现，跟一对多步骤是一样，区别就在于sql语句**

![多对多](https://img-blog.csdnimg.cn/efa2a389f6a64fdd86e6bd1274201663.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBA5LyP5Yqg54m56YGH5LiK6KW_5p-a,size_20,color_FFFFFF,t_70,g_se,x_16)


#### ① 实体和表关系

```sql
# 查询角色id=? 的角色以及对应的用户
         select * from role r inner join user_role ur inner join user u
	        on r.id = ur.rid and u.id = ur.uid
		        where r.id = ?;
 
```



#### ② User和Role实体

```java
public class Role implements Serializable {
    private Integer id;
    private String role_name;
    private String rolesc;

    private List<User> list; // 一个角色对应多个用户: 一对多
}
```

```java
public class User implements Serializable {
    private Integer id;
    private String username;
    private String birthday;
    private String sex;
    private String address;

    private List<Orders> list; // 用户对订单: 一对多
}
```

#### ③ RoleMapper接口

```java
    /**
    # 查询角色id=? 的角色以及对应的用户
         select * from role r inner join user_role ur inner join user u
	        on r.id = ur.rid and u.id = ur.uid
		        where r.id = ?;

		 1. 参数类型: Integer
		 2. 返回类型: Role
    * */
    Role findRoleByIdWithUsers(Integer rid);
```



#### ④ RoleMapper.xml

 

```xml
<!--- ************************ 一对多 ***********************************-->
    <resultMap id="myrole" type="role" autoMapping="true">
        <id property="id" column="id"/>
        <result property="rolesc" column="role_desc"/>

        <collection property="list" ofType="user" autoMapping="true">
            <id property="id" column="uid"/>
        </collection>
    </resultMap>
    <select id="findRoleByIdWithUsers" resultMap="myrole">
        select * from role r inner join user_role ur inner join user u
	        on r.id = ur.rid and u.id = ur.uid
		        where r.id = #{rid}
    </select>
```



#### ⑤ 测试

```java
    /**
     * 一对多
     * 查询id=? 的用户以及拥有的订单
     */
    @Test
    public void findRoleByIdWithUsers(){

        //查询角色id=1的角色以及对应的用户
        Role role = roleMapper.findRoleByIdWithUsers(1);
        System.out.println("role = " + role);

    }
```





## 1.4 知识小结

```markdown
# mybatis中的多表联合查询
0. 从当前javabean角度考虑关系
1. 一对一配置：使用<resultMap>+<association>做配置
	association:
    	property：关联的实体类属性名
    	javaType：关联的实体类型（别名）
    	
2. 一对多配置：使用<resultMap>+<collection>做配置
	collection：
		property：关联的集合属性名
		ofType：关联的集合元素类型（别名）
		
多对多的配置跟一对多很相似，难度在于SQL语句的编写。
```
# 二 MyBatis嵌套查询

## 2.1 什么是嵌套查询

嵌套查询就是将原来多表的联合查询语句拆成==多个单表的查询==，再使用mybatis的语法嵌套在一起。

**举个栗子**

```markdown
* 需求：查询一个订单，与此同时查询出该订单所属的用户

* 1. 关联查询：
		select * from orders o inner join user u on o.uid = u.id where o.id = 1;
		
    * 缺点：
            sql语句编写难度大
            如果表中数据量大，笛卡尔积数量倍增，可能造成内存溢出
		
* 2. 嵌套查询：
	a.根据订单id查询订单表
		select * from orders where id = 1;	
		// 查到订单id=1的订单信息 (uid=41)
		// 结果映射到 orders对象中
	b.再根据订单表中uid（外键）查询用户表
		select * from user where id = 订单表uid;
		// 结果映射到 orders.user 中
	c.最后由mybatis框架进行嵌套组合
		
		跟子查询的区别
			1. 嵌套查询分别执行两句sql : 又有订单信息,又有对应的用户信息
			2. 子查询执行一句 : 只能查到 订单编号为1的所属的用户信息
				select * from user where id = (select uid from orders where id = 1)	
    * 优点：
            sql语句编写简单
            没有多表关联，不会产生笛卡尔积, 特别是在表数据比较多的情况, 更有优势
```

 



## 2.2 一对一==嵌套==查询

**需求：查询一个订单，与此同时查询出该订单所属的用户**

**sql语句**

```sql
-- 1.根据订单id查询订单表
	select * from orders where id = 1;
-- 2.再根据订单表中uid（外键）查询用户表
	select * from user where id = 41;
```



#### ① OrderMapper接口

```java
    /**
       # 查询id=?的订单以及对应的用户
         select * from orders o inner join user u
         on o.uid = u.id
         where o.id = ?;
        1. 参数类型: Integer id
        2. 返回值类型  Orders
       # 修改成嵌套查询
           1. 先查订单id=1 的订单信息  (uid=41)
               select * from orders where id = ? // 订单id
           2. 查uid=41的用户信息
               select * from user where id = ?  // 用户id
           3. 两个结果嵌套一起: 映射到 orders
   * */
    Orders findOrderByIdWithUser3(Integer id);
```


#### ② OrderMapper映射
![在这里插入图片描述](https://img-blog.csdnimg.cn/38aa2d30dc3c45caa5eb6e2e08b63246.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBA5LyP5Yqg54m56YGH5LiK6KW_5p-a,size_20,color_FFFFFF,t_70,g_se,x_16)


```xml
   <!--
            # 嵌套查询重点:
                0. 目的
                    select * from user where id = ?
                        映射到 orders.user属性中
                1. 编写查询user表的语句:
                    UserMapper.findUserById -> UserMapper.xml
                2. 嵌套到这里
                    association标签的两个属性
                        a. column : 条件 (结果集字段作为查询语句的参数)数据库表的列名或别名
                         b. select : 调用第二句sql执行
                            接口的权限定名.方法名
                            UserMapper.findUserById(用户id)
                注意：非关联字段可使用自动映射，关联字段映射（<result property="uid" column="uid"/>）
                     是否省略会得到不同的结果。不建议省略。
                     注意：association的column属性是当前类Orders对应表orders的外键列uid
    -->
    <resultMap id="cascadeorder" type="orders" autoMapping="true">
        <id property="id" column="id"/>
        <result property="uid" column="uid"/>
        <association property="user" javaType="user" autoMapping="true"
                     column="uid"
                     select="com.wxl52d41.demo.mapper.UserMapper.findUserById">
            <id property="id" column="uid"/>
            <result property="username" column="username"/>
        </association>
    </resultMap>

    <select id="findOrderByIdWithUser3" resultMap="cascadeorder">
        select * from orders where id = #{id}
    </select>
```



#### ③ UserMapper接口

```java
    /**
     *  select * from user where id = ?
     *  1. 参数: Integer id 用户标号
     *  2. 返回值: User
     * */
    User findUserById(Integer id);
```



#### ④ UserMapper映射

```xml
    <select id="findUserById" resultType="user">
        select * from user where id = #{id}
    </select>
```



#### ⑤ 通过mybatis进行嵌套组合



 	看 OrderMapper.xml里的嵌套设置



#### ⑥ 测试

```java
    @Test
    public void findOrderByIdWithUser3(){
        // 订单编号 = 1
        Orders orders = ordersMapper.findOrderByIdWithUser3(1);
        System.out.println("订单:" + orders);
    }
```





## 2.3 一对多嵌套查询

**需求：查询一个用户，与此同时查询出该用户具有的订单**

**sql语句**

```sql
-- 1. 先根据用户id，查询用户表（一个）
SELECT * FROM USER WHERE id = 41;
-- 2. 再根据用户id，查询订单表（多个）
SELECT * FROM orders WHERE uid = 41;
```
#### ① UserMapper接口

```java
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
```



#### ② UserMapper映射

```xml
    <resultMap id="cascadeuser" type="user" autoMapping="true">
        <id property="id" column="id"/>
        <!--重点:
                查询对应的订单信息  ->  user.list中
                column : 条件(结果集字段作为查询语句的参数)
                注意：collection的column属性是当前类User对应表user的主键列id。
        -->
        <collection property="list" ofType="orders" autoMapping="true"
                    column="id"
                    select="com.wxl52d41.demo.mapper.OrdersMapper.findOrdersByUid">
        </collection>
    </resultMap>

    <select id="findUserByIdWithOrders2" resultMap="cascadeuser">
         select * from user where user.id = #{uid}
    </select>
```

#### ③ 编写第二句sql的接口和映射文件

```java
 public interface OrderMapper {
    /**
     *  select * from orders where uid = ?;
     *
     *  参数类型: Integer id
     *  返回值类型: List<Orders>
     * */
    List<Orders> findOrdersByUid(Integer uid);
 }     
   
```



```xml
 <select id="findOrdersByUid" resultType="orders">
        select * from orders where uid = #{uid}
    </select>
```





#### ④ 通过mybatis进行嵌套组合

 		请查看UserMapper.xml文件



#### ⑤ 测试

```java
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
```
![在这里插入图片描述](https://img-blog.csdnimg.cn/47524fb873ea4e338614a71cf6ca816f.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBA5LyP5Yqg54m56YGH5LiK6KW_5p-a,size_20,color_FFFFFF,t_70,g_se,x_16)

## 2.4 知识小结

==resultMap元素有两个属性==
	
 - `id`：resultMap的唯一标记，被select元素引用是使用

-	`type`：最终形成哪个类的对象。可以是别名或者全限定名。和之前resultType一样，只是显示是自定义规则映射后形成某个类对象

==resultMap元素有多个子元素==
 - `id`：配置主键列和属性的关系

-	`result`：配置非主键列和属性的关系
- `association`：配置多对一、一对一关联字段列和属性关系，对应一个对象属性
-	`collection` ：配置一对多、多对多的关联字段和属性的关系，对应一个集合属性

属性	描述
| 属性 | 描述 |
|--|--|
| property	 | 需要映射到JavaBean 的属性名称 |
| javaType	| property的类型，一个完整的类名，或者是一个类型别名。如果你匹配的是一个JavaBean，那MyBatis 通常会自行检测到 |
| column	| 数据表的列名或者列别名。 |
| jdbcType|column在数据库表中的类型。这个属性只在insert,update 或delete 的时候针对允许空的列有用。JDBC 需要这项，但MyBatis 不需要。取值是JDBCType枚举的值. |
| select	| 需要映射到JavaBean 的属性名称 |
| fetchType| association、collection的属性，使用哪个查询查询属性的值，要求指定namespace+id的全名称|
| typeHandler	| 	使用这个属性可以覆写类型处理器，实现javaType、jdbcType之间的相互转换。一般可以省略，会探测到使用的什么类型的typeHandler进行处理 |
|ofType|	collection的属性，指明集合中元素的类型（即泛型类型）|

==注意事项：==
1)	在级联查询时，对于一致的JavaBean属性和数据库表字段，因为可以自动映射，所以id、result元素可以省略。
2)	在resultMap手动映射中，一个关联列可能对应多个property，建议都进行手动映射，否则会影响查询结果
3)	javaType、jdbcType、typeHandler三个属性能省略则省略

## 2.5 级联查询总结与扩展

### 1.级联查询和多表连接查询的比较及其选择

![在这里插入图片描述](https://img-blog.csdnimg.cn/8521f465f25c45c39cc775aa639e9544.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBA5LyP5Yqg54m56YGH5LiK6KW_5p-a,size_17,color_FFFFFF,t_70,g_se,x_16)
### 2.ResultType和ResultMap使用场景
1)	如果你做的是单表的查询并且封装的实体和数据库的字段一一对应   resultType
2)	如果实体封装的属性和数据库的字段不一致  resultMap
3)	使用N+1级联查询的时候   resultMap
4)	使用的是多表的连接查询  resultMap
### 3.一对一关联映射的实现
1)	实例：学生和学生证、雇员和工牌
2)	数据库层次：主键关联或者外键关联
3)	MyBatis层次：在映射文件的设置双方均使用association即可，用法相同
### 4.多对多映射的实现
1)	实例：学生和课程、用户和角色
2)	数据库层次：引入一个中间表将一个多对多转为两个一对多
3)	MyBatis层次
方法1：在映射文件的设置双方均使用collection即可，不用引入中间类
方法2：引入中间类和中间类的映射文件，按照两个一对多处理
### 5.自关联映射
1)	实例：Emp表中的员工和上级。一般是一对多关联
2)	数据库层次：外键参考当前表的主键
3)	MyBatis层次：按照一对多处理，但是增加的属性都写到一个实体类中，增加的映射也都写到一个映射文件中



```markdown
* 步骤：一对多举例
	1）先查询（一方）单表
	2）再查询（多方）单表
	3）最后由mybatis嵌套组合


一对一配置：使用<resultMap>+<association>做配置，通过column条件，执行select查询

一对多配置：使用<resultMap>+<collection>做配置，通过column条件，执行select查询

优点：1.简化sql语句编写、2.不会产生笛卡尔积

缺点: 执行两遍


开发中到底使用哪一种？
	传统开发，数据量小：使用联合查询(执行一次,传输一次)
	互联网开发，数据量大：使用嵌套查询 (执行两次,但是只需要传输一次)
		当前也有人这么玩(知道): 
			在java中先查用户，在查角色，不在使用嵌套....(执行两次,需要传输两次)
```

[mybatis官网](https://mybatis.org/mybatis-3/zh/index.html)
