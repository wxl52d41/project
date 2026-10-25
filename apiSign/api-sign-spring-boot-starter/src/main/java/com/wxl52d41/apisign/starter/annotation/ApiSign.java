package com.wxl52d41.apisign.starter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API 参数签名校验注解
 *
 * 标注在 Controller 方法（或类）上，由 ApiSignInterceptor 拦截校验。
 * 验签模式由请求 Content-Type 自动决定：application/json 走 body 模式，其余走 query/form 参数模式。
 *
 * @author wxl52d41
 * @since 1.0.0
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiSign {

    /**
     * 允许访问该接口的接入方 appId，为空代表任意已登记的 appId 均可访问
     *
     * @return appId 数组
     */
    String[] appIds() default {};

    /**
     * 指定参与签名的参数名白名单，为空代表全部参数参与签名
     *
     * @return 参数名数组
     */
    String[] includeParams() default {};
}
