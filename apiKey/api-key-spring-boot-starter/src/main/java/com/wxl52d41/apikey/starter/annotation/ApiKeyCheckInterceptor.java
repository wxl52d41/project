package com.wxl52d41.apikey.starter.annotation;

import com.wxl52d41.apikey.core.ApiKeyTemplate;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

/**
 * ApiKeyCheck 注解拦截器
 *
 * 对应 sa-token 版的 SaCheckApiKeyHandler + readApiKeyValue 链路。
 * ApiKey 的取值顺序：请求参数 -> 请求头（参数名均为 "apikey"）。
 * 去掉了 sa-token 版的 Authorization Basic 兜底（用法罕见，需要时宿主可自行扩展）。
 *
 * @author wxl52d41
 * @since 1.0.0
 */
public class ApiKeyCheckInterceptor implements HandlerInterceptor {

    /**
     * ApiKey 的读取参数名 / 请求头名
     */
    public static final String API_KEY_NAME = "apikey";

    private final ApiKeyTemplate apiKeyTemplate;

    public ApiKeyCheckInterceptor(ApiKeyTemplate apiKeyTemplate) {
        this.apiKeyTemplate = apiKeyTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        ApiKeyCheck apiKeyCheck = handlerMethod.getMethodAnnotation(ApiKeyCheck.class);
        if (apiKeyCheck == null) {
            apiKeyCheck = handlerMethod.getBeanType().getAnnotation(ApiKeyCheck.class);
        }
        if (apiKeyCheck == null) {
            return true;
        }

        // 1. 取 ApiKey：优先请求参数，其次请求头
        String apiKey = request.getParameter(API_KEY_NAME);
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = request.getHeader(API_KEY_NAME);
        }

        // 2. 校验有效性 + Scope
        if (apiKeyCheck.mode() == ApiKeyCheck.Mode.AND) {
            apiKeyTemplate.checkApiKeyScope(apiKey, apiKeyCheck.scope());
        } else {
            apiKeyTemplate.checkApiKeyScopeOr(apiKey, apiKeyCheck.scope());
        }
        return true;
    }

    /**
     * 判断是否声明了指定 scope（供宿主在业务代码里做主动校验）
     *
     * @param apiKeyCheck 注解
     * @param scope scope
     * @return 是否声明
     */
    public static boolean hasScope(ApiKeyCheck apiKeyCheck, String scope) {
        return Arrays.asList(apiKeyCheck.scope()).contains(scope);
    }
}
