package com.wxl52d41.apisign.starter.annotation;

import com.wxl52d41.apisign.core.AppSecretProvider;
import com.wxl52d41.apisign.core.SignApp;
import com.wxl52d41.apisign.core.SignErrorCode;
import com.wxl52d41.apisign.core.SignException;
import com.wxl52d41.apisign.core.SignTemplate;
import com.wxl52d41.apisign.core.SignUtil;
import com.wxl52d41.apisign.starter.web.SignParamResolver;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Map;

/**
 * ApiSign 注解拦截器
 *
 * 处理流程：取 appId -> 校验接入方是否登记 / 启用 -> 校验接口级授权 -> 验签
 * appId 本身不参与签名，它决定使用哪套密钥。
 *
 * @author wxl52d41
 * @since 1.0.0
 */
public class ApiSignInterceptor implements HandlerInterceptor {

    private final SignTemplate signTemplate;

    private final SignParamResolver signParamResolver;

    private final AppSecretProvider appSecretProvider;

    /**
     * 接入方标识的请求头名称
     */
    private final String appIdHeader;

    public ApiSignInterceptor(SignTemplate signTemplate,
                              SignParamResolver signParamResolver,
                              AppSecretProvider appSecretProvider,
                              String appIdHeader) {
        this.signTemplate = signTemplate;
        this.signParamResolver = signParamResolver;
        this.appSecretProvider = appSecretProvider;
        this.appIdHeader = appIdHeader;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        ApiSign apiSign = handlerMethod.getMethodAnnotation(ApiSign.class);
        if (apiSign == null) {
            apiSign = handlerMethod.getBeanType().getAnnotation(ApiSign.class);
        }
        if (apiSign == null) {
            return true;
        }

        // 1. 取 appId：优先请求头，其次请求参数，便于第三方按自己习惯传递
        String appId = request.getHeader(appIdHeader);
        if (SignUtil.isEmpty(appId)) {
            appId = request.getParameter("appId");
        }
        if (SignUtil.isEmpty(appId)) {
            throw new SignException("缺少 appId，请通过请求头 " + appIdHeader + " 传递", SignErrorCode.CODE_12207);
        }

        // 2. 查接入方配置
        SignApp app = appSecretProvider.getApp(appId);
        if (app == null) {
            throw new SignException("未登记的 appId：" + appId, SignErrorCode.CODE_12208);
        }
        if (!app.isEnabled()) {
            throw new SignException("appId 已停用：" + appId, SignErrorCode.CODE_12209);
        }

        // 3. 接口级授权：注解声明了 appIds 时做白名单校验
        if (apiSign.appIds().length > 0 && !Arrays.asList(apiSign.appIds()).contains(appId)) {
            throw new SignException("appId 无权访问该接口：" + appId, SignErrorCode.CODE_12210);
        }

        // 4. 解析参数并验签
        Map<String, String> paramMap = signParamResolver.resolve(request, apiSign.includeParams());
        signTemplate.checkParamMap(paramMap, app);
        return true;
    }
}
