package com.wxl52d41.apisign.sample.demo;

import com.wxl52d41.apisign.core.SignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 签名异常统一处理示例
 *
 * 组件本身不自带全局异常处理器，避免与宿主冲突，由接入方按自己的规范处理
 *
 * @author wxl52d41
 * @since 1.0.0
 */
@RestControllerAdvice
public class SignExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SignExceptionHandler.class);

    @ExceptionHandler(SignException.class)
    public ResponseEntity<Map<String, Object>> handleSignException(SignException e) {
        log.warn("签名校验未通过: code={}, message={}", e.getCode(), e.getMessage());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", e.getCode());
        body.put("message", e.getMessage());
        // 参数缺失/格式类 400，签名无效/重放/超窗/授权失败 401
        HttpStatus status = (e.getCode() == 12204 || e.getCode() == 12206 || e.getCode() == 12207)
                ? HttpStatus.BAD_REQUEST : HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(status).body(body);
    }
}
