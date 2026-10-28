package com.wxl52d41.apikey.starter.autoconfigure;

import com.wxl52d41.apikey.core.ApiKeyConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * API Key 配置绑定
 *
 * @author wxl52d41
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "api.key")
public class ApiKeyProperties {

    /**
     * 是否启用 API Key 校验，默认启用
     */
    private boolean enabled = true;

    /**
     * API Key 前缀，默认 AK-
     */
    private String prefix = "AK-";

    /**
     * API Key 有效期（秒），-1=永久有效，默认30天
     */
    private long timeout = 2592000;

    /**
     * 是否记录索引信息（按 loginId 反查 Key 列表）
     */
    private Boolean isRecordIndex = true;

    public boolean isEnabled() {
        return enabled;
    }

    public ApiKeyProperties setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String getPrefix() {
        return prefix;
    }

    public ApiKeyProperties setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    public long getTimeout() {
        return timeout;
    }

    public ApiKeyProperties setTimeout(long timeout) {
        this.timeout = timeout;
        return this;
    }

    public Boolean getIsRecordIndex() {
        return isRecordIndex;
    }

    public ApiKeyProperties setIsRecordIndex(Boolean isRecordIndex) {
        this.isRecordIndex = isRecordIndex;
        return this;
    }

    /**
     * 转为核心层配置对象
     *
     * @return ApiKeyConfig
     */
    public ApiKeyConfig toApiKeyConfig() {
        return new ApiKeyConfig()
                .setPrefix(prefix)
                .setTimeout(timeout)
                .setIsRecordIndex(isRecordIndex);
    }
}
