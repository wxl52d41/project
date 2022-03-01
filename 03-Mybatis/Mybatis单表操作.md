# 一 Mybatis单表查询
user表

```sql
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

insert  into `user`(`id`,`username`,`birthday`,`sex`,`address`) values (41,'老王','2019-05-27 17:47:08','男','北京'),
(42,'王小二','2019-03-02 15:09:37','女','北京金燕龙'),
(43,'老李','2019-03-04 11:34:34','女','北京修正'),
(45,'小明','2019-03-04 12:04:06','男','北京金燕龙'),
(46,'王小二','2018-09-07 17:37:26','男','北京TBD'),
(48,'小马宝莉','2019-03-08 11:44:00','女','北京修正');
```

user实体类

```java
@Data
public class User {
    private Integer id;
    private String username;
    private String birthday;
    private String sex;
    private String address;
}
```

## 1.1 resultMap标签

- 如果数据库返回结果的列名和要封装的实体的属性名完全一致的话用 resultType 属性

- 如果数据库返回结果的列名和要封装的实体的属性名有不一致的情况用 resultMap 属性
  - 使用resultMap==手动建立对象关系映射==。



#### ① UserMapper接口
```java
    /**
     * # 查询 : 查询表中所有的用户
     * 1.sql  select id,username,birthday,sex,address from user
     * 2.没有参数
     * 3.返回值类型: List<User>
     */
    List<User> findAllUsers();
```
#### ② UserMapper.xml

```markup
      # select标签的属性 : resultMap
            1.resultType : 结果集的字段名和javabean属性名一致,推荐
            2. resultMap : 结果集的字段名和javabean属性名不一致 (多表)

        # resultMap 标签 : 设置结果集映射到某个对象中
            1. 属性
                id : resultMap的id(唯一性)
                type : resultMap的类型
            2. 子标签
                id子标签: 设置主键映射  (不论是否一致,必须要写)
                result子标签: 非主键映射(如果一致,可以不写,推荐写)

            3. 子标签属性
                  property :   resultMap类型中的属性名
                  column : sql结果集的列名(字段名)
```
1.resultType : 结果集的字段名和javabean属性名一致,推荐
```xml
    <select id="findAllUsers" resultType="user">
    SELECT
	 id,username,birthday,sex,address
	FROM
    user
    </select>
   ```
  
2.resultMap : 结果集的字段名和javabean属性名不一致 (多表)
```xml
    <resultMap id="userMapper" type="user">
        <id property="id" column="uid"/>
        <result property="username" column="uname"/>
        <result property="birthday" column="ubirth"/>
        <result property="sex" column="sex"/>
        <result property="address" column="address"/>
    </resultMap>
    <!-- 返回值类型: userMapper-->
    <select id="findAllUsers" resultMap="userMapper">
    SELECT
	 id,username,birthday,sex,address
	FROM
    user
    </select>
```



#### ③ 测试

```java
    @Autowired
    private UserMapper mapper;
    @Test
    void findAll() {
        List<User> list = mapper.findAllUsers();
        for (User user : list) {
            System.out.println(user);
        }
    }
```
## 1.2  多条件查询_参数映射（二种）

**需求**

根据id和username查询user表

#### ① UserMapper接口

```markup
    /**
     * 方案一: 参数列举出来
     * 1. sql  select *  from user where username = ? and sex = ?
     * 2. 参数  String , String
     * 3. 返回值类型: List<User>
     * <p>
     * 接口和映射文件(参数映射)
     *  如果有2个以及以上,建议写参数映射  
     *
     * @Param("name") String name  : 相当于告诉mybatis映射文件,这个属性是name
     */
```
[添加@Param和不添加经过进行测试，结果并不会报错](https://blog.csdn.net/Sunshineoe/article/details/114697944?spm=1001.2101.3001.6650.6&utm_medium=distribute.pc_relevant.none-task-blog-2~default~BlogCommendFromBaidu~HighlightScore-6.queryctrv2&depth_1-utm_source=distribute.pc_relevant.none-task-blog-2~default~BlogCommendFromBaidu~HighlightScore-6.queryctrv2&utm_relevant_index=8)。区别在于下图中
![@Param](https://img-blog.csdnimg.cn/ac1299661df84f52909e8fa85dec14c4.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBA5LyP5Yqg54m56YGH5LiK6KW_5p-a,size_20,color_FFFFFF,t_70,g_se,x_16)

```java

    List<User> findUsersByNameAndSex(@Param("name") String name, @Param("sex") String sex);
    
    /**
     * 方案二: 把这些参数都封装一个javabean中
     */
    List<User> findUsersByNameAndSex2(User user);
```



#### ② UserMapper.xml

```xml
    <select id="findUsersByNameAndSex" resultType="user">
         select *  from user where username = #{name} and sex = #{sex}
    </select>

    <select id="findUsersByNameAndSex2" resultType="user">
        select *  from user where username = #{username} and sex = #{sex}
    </select>


```



#### ③ 测试

```java
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
```



## 1.3 模糊查询（四种）

**需求**

根据username模糊查询user表



#### ① UserMapper接口

```java
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
```



#### ② UserMapper.xml

```xml
	<!--
        模糊查询
        方案一: 不推荐, 耦合严重
    -->
    <select id="findUsersByKd1" resultType="user">
           select * from user where username like #{keyword}
    </select>

    <!--
       模糊查询，方式二【了解】
           mysql5.5版本之前，此拼接不支持多个单引号
           oracle数据库，除了别名的位置，其余位置都不能使用双引号
    -->
    <select id="findUsersByKd2" resultType="User">
        select * from user where username like '%' #{keyword} '%'
    </select>

    <!--
        模糊查询，方式三【此方式，会出现sql注入...】
            ${} 字符串拼接，如果接收的简单数据类型，表达式名称必须是value
    -->
    <select id="findUsersByKd3"  resultType="User">
        select * from user where username like '%${keyword}%'
    </select>

    <!--
        模糊查询，方式四【掌握】
            使用concat()函数拼接  : mysql函数可以多参数
            注意：oracle数据库 concat()函数只能传递二个参数...  可以使用函数嵌套来解决
                concat(concat('%',#{username}),'%');
    -->
    <select id="findUsersByKd4"  resultType="User">
      select * from user where username like concat('%',#{username},'%')
    </select>
```



#### ③ 测试

```java
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
```



## 1.4 ${} 与 #{} 区别【面试题】

**`${}`：底层 Statement**

1. sql与参数拼接在一起，会出现sql注入问题
2. 每次执行sql语句都会编译一次



**`#{}`：底层 PreparedStatement**

1. sql与参数分离，不会出现sql注入问题
2. sql只需要编译一次

# 二 Mybatis映射文件深入

## 环境搭建
![在这里插入图片描述](https://img-blog.csdnimg.cn/9de552ad4777485ea4c4592778ffdda0.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBA5LyP5Yqg54m56YGH5LiK6KW_5p-a,size_19,color_FFFFFF,t_70,g_se,x_16)



## 2.1 返回主键

**应用场景**

向数据库保存一个user对象后, 然后在控制台打印此新增user的主键值(id)

```markdown
# 点外卖
	1. 点一份饭 -> 产生一个订单, 美团会往订单表插入一条数据(主键)
	2. 需要返回这条记录的主键, 然后给第三方配送平台, 送外卖
```



#### ① UserMapper接口

```java
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
```



#### ② UserMapper.xml

```xml
  <!--
            方案一: 这表的主键必须是自增长的 auto_increment
                 useGeneratedKeys="true" 让自增长的主键开启返回功能
                 keyColumn="id"  user表中主键列
                 keyProperty="id" user实体主键属性

                 注意：支持主键自增类型的数据库 MySQL 和 SqlServer ， oracle不支持
    -->
    <insert id="addUser" useGeneratedKeys="true" keyColumn="id" keyProperty="id">
         insert into user values(null,#{user.username},#{user.birthday},#{user.sex},#{user.address})
    </insert>

    <!--
            方案二: <selectKey>
             keyColumn="id" user表中主键列
             keyProperty="id" user实体主键属性
             resultType="int" user实体主键属性类型
             order="AFTER"  表示此标签内部sql语句在insert执行之前（执行），还是之后执行（执行）
                AFTER 之后执行【在自增主键时】
                BEFORE 之前执行【使用指定主键时】
    -->
    <insert id="addUser2">
        <selectKey keyColumn="id" keyProperty="id" resultType="int" order="AFTER">
            SELECT LAST_INSERT_ID()
        </selectKey>
        insert into user values(null, #{username},#{birthday},#{sex},#{address})
    </insert>
```



#### ③ 测试

```java
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
```





## 2.2 动态SQL

### 2.2.1 什么是动态SQL

**需求**

把id和username封装到user对象中，将user对象中不为空的属性作为查询条件
![在这里插入图片描述](https://img-blog.csdnimg.cn/670dc063e9d34d6383a38bdb143af393.png)


这个时候我们执行的sql就有多种可能

```sql
-- 如果id和用户名不为空
select * from user where id= #{id} and username = #{username}

-- 如果只有id
select * from user where id= #{id} 

-- 如果只有用户名
select * from user where username = #{username}

-- 如果id和用户名都为空
select * from user
```

像上面这样, 根据传入的参数不同, 需要执行的SQL的结构就会不同，这就是动态SQL



### 2.2.2 if 条件判断

**需求**

把id和username封装到user对象中，将user对象中不为空的属性作为查询条件



#### ① UserMapper接口

```java
 /*
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

```



#### ② UserMapper.xml

```xml
    <!--
        java 逻辑:
            username = null;  都没有意义
            username = "";

            if(username != null && !username.equals("")){
                 and username = ?
            }

      sql逻辑:
           if标签: 必要属性test (写判断条件)
                满足条件,拼接sql

           where标签: 根据最终的条件,动态修改成合适的语法

    -->
    <select id="findUsersByIdAndUserNameIf" resultType="user">
        select * from user
        <where>
            <if test="id != null and id != ''">
                id = #{id}
            </if>
            <if test="username != null and username != ''">
               and username = #{username}
            </if>
        </where>
    </select>
```



#### ③ 测试

```java
    /**
     * # 动态sql
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
```



### 2.2.3 set 用于update语句

**需求**

动态更新user表数据，如果该属性有值就更新，没有值不做处理



#### ① UserMapper接口

```java
   /*
    * # 动态sql2
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
```



#### ② UserMapper.xml

```xml
    <!--
        set标签: 用在update语句中,动态修改sql语法
    -->
    <update id="updateUserById">
        update user
        <set>
            <if test="username != null and username != ''">
               username = #{username},
            </if>
            <if test="birthday != null and birthday != ''">
                birthday = #{birthday},
            </if>
            <if test="sex != null and sex != ''">
                sex = #{sex},
            </if>
            <if test="address != null and address != ''">
                address = #{address}
            </if>
        </set>
         where id = #{id}
    </update>
```



#### ③ 测试

```java
    /**
     * # 动态sql2
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
```
### 2.2.4 foreach 用于循环遍历【重点】

**需求**

根据多个id查询，user对象的集合

```sql
select * from user where id in (41,43,46);
```

```markdown
 <foreach>标签用于遍历集合，它的属性：

    • collection：代表要遍历的集合元素

    • open：代表语句的开始部分

    • close：代表结束部分

    • item：代表遍历集合的每个元素，生成的变量名

    • sperator：代表分隔符
```



**练习二个版本**

1. 普通list集合
2. 普通array数组



#### ① UserMapper

```java
   /**
     * # 动态sql3
     *   需求: 查询某几个用户信息
     *       select * from user where id in
     *            (41,43,46)   -> 动态的
     *
     *   参数: List<Integer> list / int[] array
     *   返回值: List<User>
     * */
    List<User> findUsersByIds(List<Integer> list);

    List<User> findUsersByIds2(int[] array);
```



#### ② UserMapper.xml

```xml
   <!--
        foreach 标签(遍历)
            1. collection属性: 被遍历的容器类型
                list/array
            2. item : 被遍历出来的元素
            3. open: 遍历开始时的内容
            4. close: 遍历结束的内容
            5. separator : 每遍历一次就添加一次的分隔符(最后一次遍历不加)

        距离: list = {1,2,3}
             遍历: (1,2,3)
    -->
    <select id="findUsersByIds" resultType="user">
        select * from user where id in
        <foreach collection="list" item="id" open="(" close=")" separator=",">
            #{id}
        </foreach>
    </select>

    <select id="findUsersByIds2" resultType="user">
        select * from user where id in
        <foreach collection="array" item="id" open="(" close=")" separator=",">
            #{id}
        </foreach>
    </select>

```
#### ③ 测试

```java
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
```





## 2.3 SQL片段

**应用场景**

映射文件中可将重复的 sql 提取出来，使用时用 include 引用即可，最终达到 sql 重用的目的



```xml
   <select id="findUsersByIds2" resultType="user">
        <include refid="aa"/>
        where id in
        <foreach collection="array" item="id" open="(" close=")" separator=",">
            #{id}
        </foreach>

    </select>
    
    <!--
        sql片段: 抽取重复出现的sql语句
        在其他的语句中可以
            include标签进行引用
    -->
    <sql id="aa">
         select * from user
    </sql>
```





## 2.4 知识小结

MyBatis映射文件配置

```xml
<select>：查询

<insert>：插入

<update>：修改

<delete>：删除

<selectKey>：插入返回主键

<where>：where条件

<if>：if判断

<foreach>：for循环

<set>：set设置

<sql>：sql片段抽取
```


[mybatis官网](https://mybatis.org/mybatis-3/zh/index.html)

