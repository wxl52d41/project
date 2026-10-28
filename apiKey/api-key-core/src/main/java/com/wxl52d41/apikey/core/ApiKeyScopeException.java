package com.wxl52d41.apikey.core;

/**
 * 一个异常：代表 ApiKey Scope 相关错误
 *
 * 拷贝自 sa-token 的 ApiKeyScopeException
 *
 * @author click33 / wxl52d41
 * @since 1.0.0
 */
public class ApiKeyScopeException extends ApiKeyException {

    /**
     * 序列化版本号
     */
    private static final long serialVersionUID = 6806129545290130114L;

    /**
     * 具体引起异常的 scope 值
     */
    private String scope;

    /**
     * 具体引起异常的 ApiKey 值（子类自有副本，与父类私有字段区分）
     */
    private String apiKey;

    /**
     * 一个异常：代表 ApiKey Scope 相关错误
     *
     * @param message 异常描述
     */
    public ApiKeyScopeException(String message) {
        super(message);
    }

    /**
     * 一个异常：代表 ApiKey Scope 相关错误
     *
     * @param cause 根异常原因
     */
    public ApiKeyScopeException(Throwable cause) {
        super(cause);
    }

    public String getApiKey() {
        return apiKey;
    }

    /**
     * 覆写以维持链式调用的具体类型（setApiKey().setScope().setCode() 不断链）
     *
     * @param apiKey apiKey
     * @return 对象自身
     */
    @Override
    public ApiKeyScopeException setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public String getScope() {
        return scope;
    }

    public ApiKeyScopeException setScope(String scope) {
        this.scope = scope;
        return this;
    }

    /**
     * 覆写以维持链式调用的具体类型
     *
     * @param code 码
     * @return 对象自身
     */
    @Override
    public ApiKeyScopeException setCode(int code) {
        super.setCode(code);
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
            throw new ApiKeyScopeException(message).setCode(code);
        }
    }
}
