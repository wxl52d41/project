package com.wxl52d41.apikey.core;

/**
 * 定义 api-key 模块所有异常细分状态码
 *
 * 拷贝自 sa-token 的 SaApiKeyErrorCode，码值保持一致
 *
 * @author click33 / wxl52d41
 * @since 1.0.0
 */
public interface ApiKeyErrorCode {

    /**
     * 无效 API Key
     */
    int CODE_12301 = 12301;

    /**
     * API Key 已过期
     */
    int CODE_12302 = 12302;

    /**
     * API Key 已被禁用
     */
    int CODE_12303 = 12303;

    /**
     * API Key 字段自检未通过
     */
    int CODE_12304 = 12304;

    /**
     * 未开启索引记录功能却调用了相关 API
     */
    int CODE_12305 = 12305;

    /**
     * API Key 不具有指定 Scope
     */
    int CODE_12311 = 12311;

    /**
     * API Key 不属于指定用户
     */
    int CODE_12312 = 12312;
}
