package com.wxl52d41.apikey.core;

/**
 * API Key 数据加载器
 *
 * 拷贝自 sa-token 的 SaApiKeyDataLoader：getApiKeyModelFromDatabase 为空实现时，
 * 组件只依赖 ApiKeyStore（缓存层）；提供 DB 直查能力时缓存 miss 可回源数据库。
 *
 * 与 ApiKeyStore 的关系：Store 负责「存」，DataLoader 负责「存之外的查」（可选扩展）。
 *
 * @author click33 / wxl52d41
 * @since 1.0.0
 */
public interface ApiKeyDataLoader {

    /**
     * 根据 apiKey 从数据库获取 ApiKeyModel 信息（实现此方法无需为数据做缓存处理，由宿主决定缓存策略）
     *
     * @param apiKey apiKey
     * @return 模型，查不到返回 null
     */
    default ApiKeyModel getApiKeyModelFromDatabase(String apiKey) {
        return null;
    }
}
