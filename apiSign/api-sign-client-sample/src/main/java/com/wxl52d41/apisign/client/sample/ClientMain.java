package com.wxl52d41.apisign.client.sample;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 第三方调用演示入口
 *
 * 运行前先启动 api-sign-sample-spring-boot-starter（默认 8080），然后执行本类 main。
 *
 * @author wxl52d41
 * @since 1.0.0
 */
public class ClientMain {

    public static void main(String[] args) throws Exception {
        String baseUrl = args.length > 0 ? args[0] : "http://localhost:8080";

        // 服务端分配给我的 appId 与密钥（由服务方提供）
        ApiCallerSample caller = new ApiCallerSample(
                baseUrl, "thirdA", "thirdA-secret-key-please-change", "hmac-sha256");

        System.out.println("=== 1. POST 表单（参数模式）===");
        Map<String, String> form = new LinkedHashMap<>();
        form.put("name", "nzxy");
        form.put("age", "18");
        System.out.println(caller.postForm("/demo/form-body", form));

        System.out.println("\n=== 2. GET query ===");
        System.out.println(caller.getQuery("/demo/form/query", form));

        System.out.println("\n=== 3. GET + 路径参数 ===");
        Map<String, String> queryOnly = new LinkedHashMap<>();
        queryOnly.put("name", "nzxy");
        System.out.println(caller.getWithPathVariable("/demo/form/path/", "goodsId", "G001", queryOnly));

        System.out.println("\n=== 4. POST JSON（扁平）===");
        String json = "{\"name\":\"nzxy\",\"age\":18}";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "nzxy");
        body.put("age", 18);
        System.out.println(caller.postJson("/demo/json-body", json, body));

        System.out.println("\n=== 5. POST JSON（嵌套展平）===");
        String nestedJson = "{\"user\":{\"name\":\"nzxy\",\"age\":18},\"remark\":\"参与签名\"}";
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("name", "nzxy");
        user.put("age", 18);
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("user", user);
        nested.put("remark", "参与签名");
        System.out.println(caller.postJson("/demo/json-nested", nestedJson, nested));

        System.out.println("\n=== 6. PUT JSON ===");
        System.out.println(caller.putJson("/demo/form/json-put", json, body));
    }
}
