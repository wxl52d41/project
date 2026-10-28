package com.wxl52d41.apikey.sample.controller;

import com.wxl52d41.apikey.core.ApiKeyModel;
import com.wxl52d41.apikey.core.ApiKeyTemplate;
import com.wxl52d41.apikey.starter.annotation.ApiKeyCheck;
import com.wxl52d41.apikey.starter.annotation.ApiKeyCheckInterceptor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 受 ApiKey 保护的资源接口：演示 @ApiKeyCheck 的各种用法
 *
 * 对应 sa-token demo 的 ApiKeyResourcesController。
 * 注意：读取 apikey 必须走「param -> header」回退逻辑（与拦截器一致），
 * 直接声明 String apikey 参数只能从 query 绑定，header 方式会拿到 null
 *
 * @author wxl52d41
 * @since 1.0.0
 */
@RestController
public class ApiKeyResourceController {

    private final ApiKeyTemplate apiKeyTemplate;

    public ApiKeyResourceController(ApiKeyTemplate apiKeyTemplate) {
        this.apiKeyTemplate = apiKeyTemplate;
    }

    /**
     * 必须携带有效的 ApiKey 才能访问
     *
     * @param request 请求
     * @return 当前 Key 信息
     */
    @ApiKeyCheck
    @RequestMapping("/akRes1")
    public Map<String, Object> akRes1(HttpServletRequest request) {
        return result(readApiKey(request));
    }

    /**
     * 必须携带有效的 ApiKey，且具有 userinfo 权限
     *
     * @param request 请求
     * @return 当前 Key 信息
     */
    @ApiKeyCheck(scope = "userinfo")
    @RequestMapping("/akRes2")
    public Map<String, Object> akRes2(HttpServletRequest request) {
        return result(readApiKey(request));
    }

    /**
     * 必须携带有效的 ApiKey，且同时具有 userinfo、chat 权限（AND）
     *
     * @param request 请求
     * @return 当前 Key 信息
     */
    @ApiKeyCheck(scope = {"userinfo", "chat"})
    @RequestMapping("/akRes3")
    public Map<String, Object> akRes3(HttpServletRequest request) {
        return result(readApiKey(request));
    }

    /**
     * 必须携带有效的 ApiKey，且具有 userinfo、chat 其中之一权限（OR）
     *
     * @param request 请求
     * @return 当前 Key 信息
     */
    @ApiKeyCheck(scope = {"userinfo", "chat"}, mode = ApiKeyCheck.Mode.OR)
    @RequestMapping("/akRes4")
    public Map<String, Object> akRes4(HttpServletRequest request) {
        return result(readApiKey(request));
    }

    /**
     * 从请求读取 apikey：优先 query 参数，其次请求头（与拦截器取值顺序一致）
     *
     * @param request 请求
     * @return 校验通过的模型
     */
    private ApiKeyModel readApiKey(HttpServletRequest request) {
        String apikey = request.getParameter(ApiKeyCheckInterceptor.API_KEY_NAME);
        if (apikey == null || apikey.isEmpty()) {
            apikey = request.getHeader(ApiKeyCheckInterceptor.API_KEY_NAME);
        }
        return apiKeyTemplate.checkApiKey(apikey);
    }

    private Map<String, Object> result(ApiKeyModel akModel) {
        Map<String, Object> m = new HashMap<>();
        m.put("code", 200);
        m.put("loginId", akModel.getLoginId());
        m.put("title", akModel.getTitle());
        m.put("scopes", akModel.getScopes());
        return m;
    }
}
