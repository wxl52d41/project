package com.wxl52d41.apikey.core;

/**
 * 一个异常：代表 ApiKey 相关错误
 *
 * 拷贝自 sa-token 的 ApiKeyException，去掉 SaTokenException 父类，改为独立 RuntimeException
 *
 * @author click33 / wxl52d41
 * @since 1.0.0
 */
public class ApiKeyException extends RuntimeException {

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 6806129545290130114L;

    /**
     * 异常细分码
     */
    private int code = ApiKeyErrorCode.CODE_12301;

    /**
     * 具体引起异常的 ApiKey 值
     */
    private String apiKey;

    /**
     * 一个异常：代表 ApiKey 相关错误
     *
     * @param message 异常描述
     */
    public ApiKeyException(String message) {
        super(message);
    }

    /**
     * 一个异常：代表 ApiKey 相关错误
     *
     * @param cause 根异常原因
     */
    public ApiKeyException(Throwable cause) {
        super(cause);
    }

    public String getApiKey() {
        return apiKey;
    }

    /**
     * 注意：子类 ApiKeyScopeException 覆写了本方法以维持链式调用的具体类型
     *
     * @param apiKey apiKey
     * @return 对象自身
     */
    public ApiKeyException setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public int getCode() {
        return code;
    }

    public ApiKeyException setCode(int code) {
        this.code = code;
        return this;
    }

    /**
     * 如果 flag==true，则抛出 message 异常
     *
     * @param flag 标记
     * @param message 异常信息
     * @param code 异常细分码
     */
    public static void throwBy(boolean flag, String message, int code) {
        if (flag) {
            throw new ApiKeyException(message).setCode(code);
        }
    }
}
