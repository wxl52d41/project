package com.wxl52d41.apisign.starter.annotation;

import com.wxl52d41.apisign.core.AppSecretProvider;
import com.wxl52d41.apisign.core.SignApp;
import com.wxl52d41.apisign.core.SignErrorCode;
import com.wxl52d41.apisign.core.SignException;
import com.wxl52d41.apisign.core.SignTemplate;
import com.wxl52d41.apisign.core.SignUtil;
import com.wxl52d41.apisign.starter.web.RepeatableReadFilter;
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

        // 4. 验签：JSON body 原文模式走独立分支，其余走参数 / 展平验签
        if (apiSign.bodyMode() == ApiSign.BodyMode.RAW && RepeatableReadFilter.isJsonBody(request)) {
            checkRawBodySign(request, app);
            return true;
        }

        Map<String, String> paramMap = signParamResolver.resolve(request, apiSign.includeParams());
        signTemplate.checkParamMap(paramMap, app);
        return true;
    }

    /**
     * body 原文模式验签：sign = digest(timestamp + "\n" + nonce + "\n" + body 原文)
     *
     * 校验顺序与 checkParamMap 保持一致：三件套非空 → timestamp 窗口 → nonce 一次性 → sign 比对
     *
     * @param request 请求
     * @param app     接入方配置
     */
    private void checkRawBodySign(HttpServletRequest request, SignApp app) {
        // 三件套从 header 读取
        String timestamp = request.getHeader(SignTemplate.timestamp);
        String nonce = request.getHeader(SignTemplate.nonce);
        String sign = request.getHeader(SignTemplate.sign);
        SignException.notEmpty(timestamp, "缺少 timestamp 字段");
        SignException.notEmpty(nonce, "缺少 nonce 字段");
        SignException.notEmpty(sign, "缺少 sign 字段");

        // timestamp 校验
        signTemplate.checkTimestamp(signTemplate.parseTimestamp(timestamp), app);

        // nonce 一次性校验
        signTemplate.checkNonce(nonce, app);

        // sign 比对：对 body 原文重算签名
        String rawBody = signParamResolver.readBody(request);
        String theSign = signTemplate.createBodySign(timestamp, nonce, rawBody, app);
        if (!theSign.equals(sign)) {
            throw new SignException("无效签名：" + sign, SignErrorCode.CODE_12202);
        }
    }
}
