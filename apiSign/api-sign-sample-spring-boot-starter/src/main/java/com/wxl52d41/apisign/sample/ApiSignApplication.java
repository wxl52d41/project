package com.wxl52d41.apisign.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 服务端示例：验证 api-sign-spring-boot-starter 自动装配开箱可用
 *
 * 本类没有任何 @Import / @EnableXxx，验签能力由 starter 的 spring.factories 自动装载。
 *
 * @author wxl52d41
 * @since 1.0.0
 */
@SpringBootApplication
public class ApiSignApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiSignApplication.class, args);
    }
}
