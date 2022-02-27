# 一、为什么要使用validator?


参数校验在开发过程中是非常重要的一步，如果说前端的参数校验是为了用户的体验，那么后端的参数校验则为了安全。
小伙伴们有没有做过参数校验的土方法，一个一个字段去判断。这种方式弊端显而易见----代码冗余，我之前就这么干过。
后来通过学习发现有现成的validator工具为什么不直接使用呢？



# 二、hibernate-validator使用步骤
   

## 2.1 定义controller全局异常处理器
使用==validator==进行参数校验时，会对不符合条件的参数进行异常抛出。
如果不加以处理异常会一直抛到前端掉用处。为了优雅的处理异常，
我们需要定义全局controller异常处理器。
ControllerExceptionHandler

```java
@RestControllerAdvice
@Slf4j
public class ControllerExceptionHandler {
	@ExceptionHandler(Exception.class)
	public Result<?> handleException(Exception e){
		log.error(e.getMessage(), e);
		return Result.error(e.getMessage());
	}
}
```

## 2.2 定义新增和更新验证器

 - InsertValidator：新增时对应的字段校验生效
 
 - UpdateValidator：更新时对应的字段校验生效
![在这里插入图片描述](https://img-blog.csdnimg.cn/db276393ca064cb5aaff5db0b945d4cc.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBA5LyP5Yqg54m56YGH5LiK6KW_5p-a,size_20,color_FFFFFF,t_70,g_se,x_16)
InsertValidator
```java
/**
 * 新增验证器
 *
 */
public interface InsertValidator {

}

```
   UpdateValidator
```java
/**
 * 编辑验证器
 *
 */
public interface UpdateValidator {

}

```


## 2.3  在实体类中添加校验条件
JSR 303只是个规范，并没有具体的实现，目前通常都是由hibernate-validator进行统一参数校验。

==JSR303定义的基础校验类型：==

 - `@Null`	被注释的元素必须为 null
 - `@NotNull`	被注释的元素必须不为 null
- `@AssertTrue`	被注释的元素必须为 true
- `@AssertFalse`	被注释的元素必须为 false
- `@Min(value)`	被注释的元素必须是一个数字，其值必须大于等于指定的最小值
- `@Max(value)`	被注释的元素必须是一个数字，其值必须小于等于指定的最大值
- `@DecimalMin(value)`	被注释的元素必须是一个数字，其值必须大于等于指定的最小值
- `@DecimalMax(value)`	被注释的元素必须是一个数字，其值必须小于等于指定的- 最大值
- `@Size(max, min)`	被注释的元素的大小必须在指定的范围内
- `@Digits (integer, fraction)`	被注释的元素必须是一个数字，其值必须在可接受的范围内
- `@Past`	被注释的元素必须是一个过去的日期
- `@Future`	被注释的元素必须是一个将来的日期
- `@Pattern(value)`	被注释的元素必须符合指定的正则表达式
 
==Hibernate Validator 中附加的 constraint ：==
- `@Email`	被注释的元素必须是电子邮箱地址
- `@Length`	被注释的字符串的大小必须在指定的范围内
- `@NotEmpty`	被注释的字符串的必须非空
- `@Range`	被注释的元素必须在合适的范围内
 
```java
@Data
public class User {


    @NotNull(groups = { UpdateValidator.class })
    private Long id;// ID
    /**
     * 新增时校验条件生效
     * 用户名不能为空，且用户名个数在1-20之间。
     * */
    @NotEmpty(message = "username必填",groups = { InsertValidator.class })
    @Size(max = 20, min = 1,message = "用户名个数在1-20之间", groups = { InsertValidator.class })
    private String username;// 用户名

    @NotBlank(message = "密码必填",groups = { InsertValidator.class })
    @Size(min = 6,message = "密码个数不少于六位", groups = { InsertValidator.class })
    private String password;// 密码

    /**
     * 新增时校验条件生效
     * 邮箱校验规则
     * */
    @NotBlank(message = "邮箱不能为空",groups = { InsertValidator.class })
    @Email(groups = { InsertValidator.class })
    @Size(max = 30,message = "邮箱个数不多于30位", groups = { InsertValidator.class })
    private String email;// 邮箱

    /**
     * 新增时校验条件生效
     * 使用了手机号校验规则
     * */
    @NotBlank(message = "手机号不能为空",groups = { InsertValidator.class })
    @TelephoneNumber(groups = { InsertValidator.class },message = "手机号不正确")
    @Size(max = 20, groups = { InsertValidator.class })
    private String mobile;// 手机号

    @Size(max = 20, min = 6, groups = {InsertValidator.class, UpdateValidator.class })
    private String provinceRegionCode;// 省份区域代码

    @Size(max = 20, min = 6, groups = { UpdateValidator.class })
    private String cityRegionCode;// 地市区域代码

    @Size(max = 40, groups = { UpdateValidator.class })
    private String address;// 街道地址

    @Size(max = 256, groups = { UpdateValidator.class })
    private String profile;// 个人简介

}

```

## 2.4 在controller中添加注解
 - @Validated(InsertValidator.class) 所有group中使用InsertValidator的属性校验效
 - @Validated(UpdateValidator.class)所有group中使用UpdateValidator的属性校验生效

```java
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

```

## 2.5  测试结果
### 2.5.1 执行insertUser操作
 1. `username` 校验
![在这里插入图片描述](https://img-blog.csdnimg.cn/6863202fdf6548568dc7285d33e6d303.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBA5LyP5Yqg54m56YGH5LiK6KW_5p-a,size_20,color_FFFFFF,t_70,g_se,x_16)

==入参 名称为空时==

```json
{
  "id": 1,
  "username": "",
  "password": "demoData",
  "email": "1234@qq.com",
  "mobile": "18898987878",
  "provinceRegionCode": "demoData",
  "cityRegionCode": "demoData",
  "address": "demoData",
  "profile": "demoData"
}
```
==结果==

```json
{
    "success": false,
    "message": "username必填; 用户名个数在1-20之间",
    "code": 400,
    "result": null,
    "timestamp": 1645888982098
}
```

 2. `password` 校验

![在这里插入图片描述](https://img-blog.csdnimg.cn/937e1521d92c457c856c75ab90ce8cf7.png)

1.密码为空时

```json
{
  "id": 1,
  "username": "小明",
  "password": "",
  "email": "1234@qq.com",
  "mobile": "18898987878",
  "provinceRegionCode": "demoData",
  "cityRegionCode": "demoData",
  "address": "demoData",
  "profile": "demoData"
}
```
结果

```json
{
    "success": false,
    "message": "密码个数不少于六位; 密码必填",
    "code": 400,
    "result": null,
    "timestamp": 1645889419709
}
```

 2.密码个数少于六位时
 

```json
{
  "id": 1,
  "username": "小明",
  "password": "123",
  "email": "1234@qq.com",
  "mobile": "18898987878",
  "provinceRegionCode": "demoData",
  "cityRegionCode": "demoData",
  "address": "demoData",
  "profile": "demoData"
}
```

 结果
 

```r
{
    "success": false,
    "message": "密码个数不少于六位",
    "code": 400,
    "result": null,
    "timestamp": 1645889483465
}
```

 
 3. 邮箱校验
 
![邮箱校验](https://img-blog.csdnimg.cn/ea700a3c211c4e13819dd889e15a3e59.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBA5LyP5Yqg54m56YGH5LiK6KW_5p-a,size_20,color_FFFFFF,t_70,g_se,x_16)
1.邮箱为空时

```json
{
  "id": 1,
  "username": "小明",
  "password": "122223",
  "email": "",
  "mobile": "18898987878",
  "provinceRegionCode": "demoData",
  "cityRegionCode": "demoData",
  "address": "demoData",
  "profile": "demoData"
}
```
结果

```json
{
    "success": false,
    "message": "邮箱不能为空",
    "code": 400,
    "result": null,
    "timestamp": 1645889785044
}
```
2.邮箱不符合规则时
```json
{
  "id": 1,
  "username": "小明",
  "password": "122223",
  "email": "1234",
  "mobile": "18898987878",
  "provinceRegionCode": "demoData",
  "cityRegionCode": "demoData",
  "address": "demoData",
  "profile": "demoData"
}
```
结果

```json
{
    "success": false,
    "message": "不是一个合法的电子邮件地址",
    "code": 400,
    "result": null,
    "timestamp": 1645889864638
}
```

 
3.手机号验证
手机号属性上使用的新增和更新时验证。当在controller方法中，添加@Validated(InsertValidator.class)和@Validated(UpdateValidator.class)时都会执行校验逻辑。
![在这里插入图片描述](https://img-blog.csdnimg.cn/c7571906f0b74042a20e0b186097d0d7.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBA5LyP5Yqg54m56YGH5LiK6KW_5p-a,size_20,color_FFFFFF,t_70,g_se,x_16)
入参
手机号超过11位时
```json
{
  "id": 1,
  "username": "小明",
  "password": "122223",
  "email": "1234@qq.com",
  "mobile": "188989878788",
  "provinceRegionCode": "demoData",
  "cityRegionCode": "demoData",
  "address": "demoData",
  "profile": "demoData"
}
```
结果

```json
{
    "success": false,
    "message": "手机号不正确; 个数必须在0和11之间",
    "code": 400,
    "result": null,
    "timestamp": 1645960757645
}
```
2.5.2 执行updateUser操作时
1.手机号验证
手机号属性上使用的新增和更新时验证。当在controller方法中，添加@Validated(UpdateValidator.class)时都会执行校验逻辑。
![在这里插入图片描述](https://img-blog.csdnimg.cn/60afd357399d473ea67906e0aff457e6.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBA5LyP5Yqg54m56YGH5LiK6KW_5p-a,size_20,color_FFFFFF,t_70,g_se,x_16)
入参
手机号超过11位时
```json
{
  "id": 1,
  "username": "demoData",
  "password": "demoData",
  "email": "demoData",
  "mobile": "188959589892",
  "provinceRegionCode": "demoData",
  "cityRegionCode": "demoData",
  "address": "demoData",
  "profile": "demoData"
}
```
结果

```json
{
    "success": false,
    "message": "个数必须在0和11之间; 手机号不正确",
    "code": 400,
    "result": null,
    "timestamp": 1645961643659
}
```

## 手机号验证使用了自定义注解

![在这里插入图片描述](https://img-blog.csdnimg.cn/f2afb3f84f2048f0b4978b9211f7bb36.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBA5LyP5Yqg54m56YGH5LiK6KW_5p-a,size_20,color_FFFFFF,t_70,g_se,x_16)

```java
/**
 * 手机号码验证器
 *
 */
@Documented
@Constraint(validatedBy = {})
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
@Retention(RUNTIME)
@ReportAsSingleViolation
@Pattern(regexp = "^((13[0-9])|(15[^4])|(18[0-9])|(17[0-9])|(147))\\d{8}$")
public @interface TelephoneNumber {

	String message() default "手机号不正确";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
```

