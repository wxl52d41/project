package com.wxl52d41.apisign.starter.web;

import com.wxl52d41.apisign.core.SignException;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JSON body 请求可重复读过滤器
 *
 * 仅当 Content-Type 为 application/json 时包装，其他请求（含 multipart 文件上传）原样透传，
 * 避免大 body 被整体缓存进内存。已被包装过的请求不会重复包装。
 *
 * @author wxl52d41
 * @since 1.0.0
 */
public class RepeatableReadFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 宿主可能已用本组件或类似组件包装过，避免重复包装导致 body 重复读取
        if (!isJsonBody(request) || request instanceof RepeatableReadRequestWrapper) {
            filterChain.doFilter(request, response);
            return;
        }

        RepeatableReadRequestWrapper wrapper;
        try {
            wrapper = new RepeatableReadRequestWrapper(request);
        } catch (SignException e) {
            // Filter 层异常不经过全局异常处理器，直接写出错误响应
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":" + e.getCode()
                    + ",\"message\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}");
            return;
        }
        filterChain.doFilter(wrapper, response);
    }

    /**
     * 是否为 application/json 请求（兼容 application/json;charset=UTF-8 等写法）
     *
     * @param request 请求
     * @return 是否 json body 请求
     */
    public static boolean isJsonBody(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().contains("application/json");
    }
}
