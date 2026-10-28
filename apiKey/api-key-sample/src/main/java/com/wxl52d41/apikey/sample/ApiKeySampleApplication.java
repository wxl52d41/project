package com.wxl52d41.apikey.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 示例应用入口
 *
 * @author wxl52d41
 * @since 1.0.0
 */
@SpringBootApplication
public class ApiKeySampleApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(ApiKeySampleApplication.class, args);

        // 对齐 sa-token demo 启动输出风格：启动后打印配置与测试地址
        System.out.println("启动成功：API Key  配置如下：" + context.getBean(
                com.wxl52d41.apikey.core.ApiKeyTemplate.class).getConfig());
        System.out.println("测试访问：http://localhost:8090/debug.html");
        System.out.println("测试接口：/akRes1 /akRes2 /akRes3 /akRes4（携带 apikey 参数或请求头）");
        System.out.println("管理接口：/createApiKey /myApiKeyList /updateApiKey /deleteApiKey /deleteMyAllApiKey");
    }
}
