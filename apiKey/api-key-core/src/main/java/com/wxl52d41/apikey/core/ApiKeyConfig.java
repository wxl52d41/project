package com.wxl52d41.apikey.core;

/**
 * API Key 配置
 *
 * 拷贝自 sa-token 的 SaApiKeyConfig，去掉 SaJsonType 依赖
 *
 * @author click33 / wxl52d41
 * @since 1.0.0
 */
public class ApiKeyConfig {

    /**
     * API Key 前缀
     */
    private String prefix = "AK-";

    /**
     * API Key 有效期，-1=永久有效，默认30天 （修改此配置项不会影响到已创建的 API Key）
     */
    private long timeout = 2592000;

    /**
     * 框架是否记录索引信息（按 loginId 反查 Key 列表）
     */
    private Boolean isRecordIndex = true;

    public String getPrefix() {
        return this.prefix;
    }

    public ApiKeyConfig setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    public long getTimeout() {
        return this.timeout;
    }

    public ApiKeyConfig setTimeout(long timeout) {
        this.timeout = timeout;
        return this;
    }

    public Boolean getIsRecordIndex() {
        return this.isRecordIndex;
    }

    public ApiKeyConfig setIsRecordIndex(Boolean isRecordIndex) {
        this.isRecordIndex = isRecordIndex;
        return this;
    }

    @Override
    public String toString() {
        return "ApiKeyConfig{" +
                "prefix='" + prefix + '\'' +
                ", timeout=" + timeout +
                ", isRecordIndex=" + isRecordIndex +
                '}';
    }
}
