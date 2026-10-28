package com.wxl52d41.apikey.starter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API Key 校验：指定请求中必须包含有效的 ApiKey ，并且包含指定的 scope
 *
 * 拷贝自 sa-token 的 SaCheckApiKey（SaMode 本地化为枚举）。
 * 可标注在方法、类上（效果等同于标注在此类的所有方法上）。
 *
 * @author click33 / wxl52d41
 * @since 1.0.0
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiKeyCheck {

    /**
     * 指定 API key 必须包含的权限 [ 数组 ]
     *
     * @return scope 数组
     */
    String[] scope() default {};

    /**
     * 验证模式：AND | OR，默认AND
     *
     * @return 模式
     */
    Mode mode() default Mode.AND;

    /**
     * 验证模式
     */
    enum Mode {
        /**
         * 全部具备
         */
        AND,
        /**
         * 具备其一
         */
        OR
    }
}
