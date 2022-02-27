package com.example.demo.config;


import com.example.demo.entity.LoginUser;
import com.example.demo.utils.ConvertUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Properties;

/**
 * mybatis拦截器，自动注入创建人、创建时间、修改人、修改时间
 *
 * @Author scott
 * @Date 2019-01-19
 */
@Slf4j
@Component
@Intercepts({@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})})
public class MybatisInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        String sqlId = mappedStatement.getId();
        log.debug("------sqlId------" + sqlId);
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
        Object parameter = invocation.getArgs()[1];
        log.debug("------sqlCommandType------" + sqlCommandType);

        if (parameter == null) {
            return invocation.proceed();
        }
        if (SqlCommandType.INSERT == sqlCommandType) {
            LoginUser sysUser = this.getLoginUser();
            Field[] fields = ConvertUtils.getAllFields(parameter);
            for (Field field : fields) {
                log.debug("------field.name------" + field.getName());
                try {
                    if ("createBy".equals(field.getName())) {
                        field.setAccessible(true);
                        Object local_createBy = field.get(parameter);
                        field.setAccessible(false);
                        if (local_createBy == null || local_createBy.equals("")) {
                            if (sysUser != null) {
                                // 登录人账号
                                field.setAccessible(true);
                                field.set(parameter, sysUser.getUsername());
                                field.setAccessible(false);
                            }
                        }
                    }
                    if ("createByName".equals(field.getName())) {
                        field.setAccessible(true);
                        Object local_createBy = field.get(parameter);
                        field.setAccessible(false);
                        if (local_createBy == null || local_createBy.equals("")) {
                            if (sysUser != null) {
                                // 登录人姓名
                                field.setAccessible(true);
                                field.set(parameter, sysUser.getRealname());
                                field.setAccessible(false);
                            }
                        }
                    }
                    // 注入创建时间
                    if ("createTime".equals(field.getName())) {
                        field.setAccessible(true);
                        Object local_createDate = field.get(parameter);
                        field.setAccessible(false);
                        if (local_createDate == null || local_createDate.equals("")) {
                            field.setAccessible(true);
                            field.set(parameter, new Date());
                            field.setAccessible(false);
                        }
                    }
                    //注入部门编码
                    if ("sysOrgCode".equals(field.getName())) {
                        field.setAccessible(true);
                        Object local_sysOrgCode = field.get(parameter);
                        field.setAccessible(false);
                        if (local_sysOrgCode == null || local_sysOrgCode.equals("")) {
                            // 获取登录用户信息
                            if (sysUser != null) {
                                field.setAccessible(true);
                                field.set(parameter, sysUser.getOrgCode());
                                field.setAccessible(false);
                            }
                        }
                    }
                } catch (Exception e) {
                }
            }
        }
        if (SqlCommandType.UPDATE == sqlCommandType) {
            LoginUser sysUser = this.getLoginUser();
            Field[] fields = null;
            if (parameter instanceof ParamMap) {
                ParamMap<?> p = (ParamMap<?>) parameter;
                //update-begin-author:scott date:20190729 for:批量更新报错issues/IZA3Q--
                if (p.containsKey("et")) {
                    parameter = p.get("et");
                } else {
                    parameter = p.get("param1");
                }
                //update-end-author:scott date:20190729 for:批量更新报错issues/IZA3Q-

                //update-begin-author:scott date:20190729 for:更新指定字段时报错 issues/#516-
                if (parameter == null) {
                    return invocation.proceed();
                }
                //update-end-author:scott date:20190729 for:更新指定字段时报错 issues/#516-

                fields = ConvertUtils.getAllFields(parameter);
            } else {
                fields = ConvertUtils.getAllFields(parameter);
            }

            for (Field field : fields) {
                log.debug("------field.name------" + field.getName());
                try {
                    if ("updateBy".equals(field.getName())) {
                        //获取登录用户信息
                        if (sysUser != null) {
                            // 登录账号
                            field.setAccessible(true);
                            field.set(parameter, sysUser.getUsername());
                            field.setAccessible(false);
                        }
                    }
                    if ("updateByName".equals(field.getName())) {
                        //获取登录用户信息
                        if (sysUser != null) {
                            // 登录账号
                            field.setAccessible(true);
                            field.set(parameter, sysUser.getRealname());
                            field.setAccessible(false);
                        }
                    }
                    if ("updateTime".equals(field.getName())) {
                        field.setAccessible(true);
                        field.set(parameter, new Date());
                        field.setAccessible(false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // TODO Auto-generated method stub
    }

    private LoginUser getLoginUser() {
        LoginUser loginUser = null;
        try {
            //模拟获取用户信息
            loginUser = new LoginUser();
            loginUser.setId("12345");
            loginUser.setUsername("xiaoming");
            loginUser.setRealname("小明");
            loginUser.setPassword("");
            loginUser.setOrgCode("开发部");
            loginUser.setAvatar("");
            loginUser.setBirthday(new Date());
            loginUser.setSex(1);
            loginUser.setEmail("12345@qq.com");
            loginUser.setPhone("18890908989");
            loginUser.setStatus(1);
        } catch (Exception e) {
            loginUser = null;
        }
        return loginUser;
    }

}
