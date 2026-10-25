package com.wxl52d41.apisign.sample.demo;

import com.wxl52d41.apisign.core.AppSecretProvider;
import com.wxl52d41.apisign.core.SignApp;
import com.wxl52d41.apisign.core.SignTemplate;
import com.wxl52d41.apisign.core.SignUtil;
import com.wxl52d41.apisign.starter.annotation.ApiSign;
import com.wxl52d41.apisign.starter.web.SignParamResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 多第三方接入演示
 *
 * 三个 appId 使用不同算法与密钥，通过请求头 X-Sign-App-Id 区分
 *
 * @author wxl52d41
 * @since 1.0.0
 */
@RestController
@RequestMapping("demo")
public class DemoController {

    private static final Logger log = LoggerFactory.getLogger(DemoController.class);

    private final SignTemplate signTemplate;

    private final AppSecretProvider appSecretProvider;

    public DemoController(SignTemplate signTemplate, AppSecretProvider appSecretProvider) {
        this.signTemplate = signTemplate;
        this.appSecretProvider = appSecretProvider;
    }

    /**
     * 生成指定 appId 的签名示例（模拟第三方客户端）
     *
     * @param appId 接入方标识，默认 thirdA
     * @return 各模式的请求示例
     */
    @GetMapping("create-demo")
    public String createDemo(String appId) {
        if (appId == null || appId.isEmpty()) {
            appId = "thirdA";
        }
        SignApp app = appSecretProvider.getApp(appId);
        if (app == null) {
            return "未登记的 appId：" + appId;
        }

        String name = "nzxy";
        int age = 18;

        // 同一组 timestamp/nonce 供各示例共用，实际调用时每个请求都要单独生成
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = SignUtil.getRandomString(32);

        Map<String, Object> paramMap = new LinkedHashMap<>();
        paramMap.put("name", name);
        paramMap.put("age", age);
        String paramSign = createSign(paramMap, app, timestamp, nonce);

        Map<String, Object> flatBody = new LinkedHashMap<>();
        flatBody.put("name", name);
        flatBody.put("age", age);
        String flatSign = createSign(flatBody, app, timestamp, nonce);

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("name", name);
        user.put("age", age);
        Map<String, Object> nestedBody = new LinkedHashMap<>();
        nestedBody.put("user", user);
        nestedBody.put("remark", "参与签名");
        String nestedSign = createSign(nestedBody, app, timestamp, nonce);

        Map<String, Object> someBody = new LinkedHashMap<>();
        someBody.put("name", name);
        someBody.put("age", age);
        String someSign = createSign(someBody, app, timestamp, nonce);

        StringBuilder sb = new StringBuilder();
        sb.append("appId=").append(app.getAppId())
                .append("  digestAlgo=").append(app.getDigestAlgo())
                .append("  timestampDisparity=").append(app.getTimestampDisparity())
                .append("  enabled=").append(app.isEnabled()).append("\n\n");

        sb.append("### 1. 参数模式（query / form）\n");
        sb.append("POST http://localhost:8080/demo/form-body\n");
        sb.append("X-Sign-App-Id: ").append(app.getAppId()).append("\n");
        sb.append("Content-Type: application/x-www-form-urlencoded\n\n");
        sb.append("name=").append(name).append("&age=").append(age)
                .append("&timestamp=").append(timestamp)
                .append("&nonce=").append(nonce)
                .append("&sign=").append(paramSign).append("\n\n");

        sb.append("### 2. JSON body 模式（扁平）\n");
        sb.append("POST http://localhost:8080/demo/json-body\n");
        sb.append("X-Sign-App-Id: ").append(app.getAppId()).append("\n");
        sb.append("timestamp: ").append(timestamp).append("\n");
        sb.append("nonce: ").append(nonce).append("\n");
        sb.append("sign: ").append(flatSign).append("\n");
        sb.append("Content-Type: application/json\n\n");
        sb.append("{\n  \"name\": \"").append(name).append("\",\n  \"age\": ").append(age).append("\n}\n\n");

        sb.append("### 3. JSON body 模式（嵌套展平）\n");
        sb.append("POST http://localhost:8080/demo/json-nested\n");
        sb.append("X-Sign-App-Id: ").append(app.getAppId()).append("\n");
        sb.append("timestamp: ").append(timestamp).append("\n");
        sb.append("nonce: ").append(nonce).append("\n");
        sb.append("sign: ").append(nestedSign).append("\n");
        sb.append("Content-Type: application/json\n\n");
        sb.append("{\n  \"user\": {\"name\": \"").append(name).append("\", \"age\": ").append(age).append("},\n");
        sb.append("  \"remark\": \"参与签名\"\n}\n\n");

        sb.append("### 4. JSON body 模式（includeParams 白名单）\n");
        sb.append("POST http://localhost:8080/demo/json-some-params\n");
        sb.append("X-Sign-App-Id: ").append(app.getAppId()).append("\n");
        sb.append("timestamp: ").append(timestamp).append("\n");
        sb.append("nonce: ").append(nonce).append("\n");
        sb.append("sign: ").append(someSign).append("\n");
        sb.append("Content-Type: application/json\n\n");
        sb.append("{\n  \"name\": \"").append(name).append("\",\n  \"age\": ").append(age).append(",\n");
        sb.append("  \"otherParam\": \"不参与签名，可任意修改\"\n}\n\n");

        sb.append("### 5. 接口级授权（仅 thirdA 可访问）\n");
        sb.append("POST http://localhost:8080/demo/only-third-a\n");
        sb.append("X-Sign-App-Id: ").append(app.getAppId()).append("\n");
        sb.append("timestamp: ").append(timestamp).append("\n");
        sb.append("nonce: ").append(nonce).append("\n");
        sb.append("sign: ").append(flatSign).append("\n");
        sb.append("Content-Type: application/json\n\n");
        sb.append("{\n  \"name\": \"").append(name).append("\",\n  \"age\": ").append(age).append("\n}\n");

        return sb.toString();
    }

    /**
     * 模拟第三方客户端：body 展平后以指定三件套生成签名
     */
    private String createSign(Map<String, Object> body, SignApp app, String timestamp, String nonce) {
        Map<String, String> flatMap = new LinkedHashMap<>();
        SignParamResolver.flattenObject(body, "", flatMap);
        Map<String, Object> signMap = new LinkedHashMap<>(flatMap);
        signMap.put(SignTemplate.timestamp, timestamp);
        signMap.put(SignTemplate.nonce, nonce);
        return signTemplate.createSign(signMap, app);
    }

    @PostMapping("form-body")
    @ApiSign
    public Map<String, Object> formBody(String name, String age) {
        log.info("form-body name:{}, age:{}", name, age);
        return result("param", "name", name, "age", age);
    }

    @PostMapping("json-body")
    @ApiSign
    public Map<String, Object> jsonBody(@RequestBody Map<String, Object> body) {
        log.info("json-body:{}", body);
        return result("json-flat", "body", body);
    }

    @PostMapping("json-nested")
    @ApiSign
    public Map<String, Object> jsonNested(@RequestBody Map<String, Object> body) {
        log.info("json-nested:{}", body);
        return result("json-nested", "body", body);
    }

    /**
     * 仅 name / age 参与签名，otherParam 变更不影响验签
     */
    @PostMapping("json-some-params")
    @ApiSign(includeParams = {"name", "age"})
    public Map<String, Object> jsonSomeParams(@RequestBody Map<String, Object> body) {
        log.info("json-some-params:{}", body);
        return result("json-include-params", "body", body);
    }

    /**
     * 仅允许 thirdA 调用，用于验证接口级授权
     */
    @PostMapping("only-third-a")
    @ApiSign(appIds = {"thirdA"})
    public Map<String, Object> onlyThirdA(@RequestBody Map<String, Object> body) {
        log.info("only-third-a:{}", body);
        return result("only-third-a", "body", body);
    }

    private Map<String, Object> result(String mode, Object... kvs) {
        Map<String, Object> m = new HashMap<>();
        m.put("mode", mode);
        for (int i = 0; i < kvs.length; i += 2) {
            m.put(String.valueOf(kvs[i]), kvs[i + 1]);
        }
        return m;
    }
}
