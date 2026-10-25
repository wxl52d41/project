package com.wxl52d41.apisign.core;

/**
 * 签名验签模块异常
 *
 * 拷贝自 sa-token 的 SaSignException
 *
 * @author click33 / wxl52d41
 * @since 1.0.0
 */
public class SignException extends RuntimeException {

    /**
     * 异常细分状态码
     */
    private int code = SignErrorCode.CODE_12202;

    public SignException(String message) {
        super(message);
    }

    public SignException(String message, int code) {
        super(message);
        this.code = code;
    }

    /**
     * 如果给定参数为空，则抛出异常
     *
     * @param value   待验证的参数
     * @param message 异常信息
     */
    public static void notEmpty(Object value, String message) {
        if (SignUtil.isEmpty(value)) {
            throw new SignException(message, SignErrorCode.CODE_12204);
        }
    }

    /**
     * 如果给定参数为空，则抛出异常
     *
     * @param value   待验证的参数
     * @param message 异常信息
     * @param code    异常状态码
     */
    public static void notEmpty(Object value, String message, int code) {
        if (SignUtil.isEmpty(value)) {
            throw new SignException(message, code);
        }
    }

    public int getCode() {
        return code;
    }

    public SignException setCode(int code) {
        this.code = code;
        return this;
    }
}
