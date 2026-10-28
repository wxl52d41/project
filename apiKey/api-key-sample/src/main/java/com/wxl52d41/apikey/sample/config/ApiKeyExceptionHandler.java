package com.wxl52d41.apikey.sample.config;

import com.wxl52d41.apikey.core.ApiKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * ApiKey 异常处理：宿主项目参考此写法（starter 不自带，避免与宿主冲突）
 *
 * @author wxl52d41
 * @since 1.0.0
 */
@RestControllerAdvice
public class ApiKeyExceptionHandler {

    /**
     * ApiKey 校验异常统一转 401
     *
     * @param e 异常
     * @return 响应
     */
    @ExceptionHandler(ApiKeyException.class)
    public ResponseEntity<Map<String, Object>> handleApiKeyException(ApiKeyException e) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", e.getCode());
        body.put("message", e.getMessage());
        body.put("apiKey", e.getApiKey());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }
}
