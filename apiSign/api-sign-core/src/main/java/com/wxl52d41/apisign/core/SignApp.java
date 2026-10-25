package com.wxl52d41.apisign.core;

/**
 * 接入方签名配置：一个 appId 对应一套密钥与算法参数
 *
 * 由 AppSecretProvider 提供，服务端按请求携带的 appId 取出对应实例参与验签
 *
 * @author wxl52d41
 * @since 1.0.0
 */
public class SignApp {

    /**
     * 接入方标识
     */
    private String appId;

    /**
     * 签名密钥
     */
    private String secretKey;

    /**
     * 摘要算法，默认 md5，可选 md5 / sha1 / sha256 / sha384 / sha512 / hmac-md5 / hmac-sha256 / hmac-sha512
     */
    private String digestAlgo = "md5";

    /**
     * 时间戳允许差距（毫秒），-1 代表不校验，默认 15 分钟
     */
    private long timestampDisparity = 1000L * 60 * 15;

    /**
     * 是否启用，停用后该 appId 的所有请求均被拒绝
     */
    private boolean enabled = true;

    public SignApp() {
    }

    public SignApp(String appId, String secretKey) {
        this.appId = appId;
        this.secretKey = secretKey;
    }

    public SignApp(String appId, String secretKey, String digestAlgo) {
        this.appId = appId;
        this.secretKey = secretKey;
        this.digestAlgo = digestAlgo;
    }

    /**
     * 计算保存 nonce 时应该使用的 ttl，单位：秒
     *
     * @return nonce 过期秒数
     */
    public long getSaveNonceExpire() {
        if (timestampDisparity >= 0) {
            return timestampDisparity / 1000;
        }
        return 60 * 60 * 24;
    }

    public String getAppId() {
        return appId;
    }

    public SignApp setAppId(String appId) {
        this.appId = appId;
        return this;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public SignApp setSecretKey(String secretKey) {
        this.secretKey = secretKey;
        return this;
    }

    public String getDigestAlgo() {
        return digestAlgo;
    }

    public SignApp setDigestAlgo(String digestAlgo) {
        this.digestAlgo = digestAlgo;
        return this;
    }

    public long getTimestampDisparity() {
        return timestampDisparity;
    }

    public SignApp setTimestampDisparity(long timestampDisparity) {
        this.timestampDisparity = timestampDisparity;
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public SignApp setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }
}
