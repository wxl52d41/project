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

    /**
     * JSON body 的签名模式（仅 Content-Type 为 application/json 时生效）：
     * - RAW（默认）：body 原文签名，sign = digest(timestamp + "\n" + nonce + "\n" + body原文)，
     *   对标 Stripe / GitHub webhook，客户端不依赖展平规则，任意语言零门槛接入
     * - FLATTEN：展平 body 字段参与签名，支持 includeParams 字段级白名单
     *
     * 默认 RAW 的理由：市场主流（Stripe / GitHub / 微信 v3）均签 body 原文；
     * 展平模式保留给需要字段级控制（includeParams）的内部对接场景
     *
     * FLATTEN 模式下 includeParams 才生效（RAW 签的是整体原文，无字段级控制）
     *
     * @return body 签名模式
     */
    BodyMode bodyMode() default BodyMode.RAW;

    /**
     * JSON body 签名模式枚举
     */
    enum BodyMode {
        /**
         * 展平模式：JSON 字段展平为点号键参与签名，支持字段级白名单
         */
        FLATTEN,
        /**
         * 原文模式：对 timestamp + "\n" + nonce + "\n" + body 原文整体签名
         */
        RAW
    }
}
