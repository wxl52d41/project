package com.wxl52d41.apisign.starter.autoconfigure;

import com.wxl52d41.apisign.core.SignApp;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * API 签名验签配置
 *
 * @author wxl52d41
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "api.sign")
public class SignProperties {

    /**
     * 是否启用签名验签，默认启用
     */
    private boolean enabled = true;

    /**
     * 请求头名称：接入方标识，默认 X-Sign-App-Id
     */
    private String appIdHeader = "X-Sign-App-Id";

    /**
     * 重复读过滤器的执行顺序，默认最高优先级
     */
    private int filterOrder = 0;

    /**
     * 接入方列表。密钥也可由自定义 AppSecretProvider 从数据库加载
     */
    private List<SignApp> apps = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public SignProperties setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String getAppIdHeader() {
        return appIdHeader;
    }

    public SignProperties setAppIdHeader(String appIdHeader) {
        this.appIdHeader = appIdHeader;
        return this;
    }

    public int getFilterOrder() {
        return filterOrder;
    }

    public SignProperties setFilterOrder(int filterOrder) {
        this.filterOrder = filterOrder;
        return this;
    }

    public List<SignApp> getApps() {
        return apps;
    }

    public SignProperties setApps(List<SignApp> apps) {
        this.apps = apps;
        return this;
    }
}
