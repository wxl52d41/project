package com.wxl52d41.apikey.core;

/**
 * API Key 存储接口
 *
 * 职责：ApiKeyModel 的持久化与按用户的索引查询。
 * 替代 sa-token 版的 SaTokenDao + SaSession 组合（本组件不再依赖 sa-token）。
 *
 * 实现要求：
 * 1. 存储必须带 TTL 语义（或等价的过期清理），key 过期后 get 返回 null
 * 2. getIndexList / saveIndexList 用于「按用户查所有 Key」的反向索引，
 *    实现可自由选择数据结构（DB 表、Redis Set 等），只需保证读写一致
 * 3. 并发安全：多实例部署时所有操作必须落到共享存储（Redis / DB），不可用内存 Map
 *
 * @author wxl52d41
 * @since 1.0.0
 */
public interface ApiKeyStore {

    /**
     * 根据 apiKey 读取模型，不存在或已过期返回 null
     *
     * @param apiKey API Key 值
     * @return 模型，无效时 null
     */
    ApiKeyModel getApiKey(String apiKey);

    /**
     * 保存模型，按模型自身的有效期设置存储过期
     *
     * @param apiKey apiKey
     * @param model  模型
     * @param ttlSeconds 剩余有效期（秒），-1 代表永不过期
     */
    void saveApiKey(String apiKey, ApiKeyModel model, long ttlSeconds);

    /**
     * 删除指定 apiKey
     *
     * @param apiKey apiKey
     */
    void deleteApiKey(String apiKey);

    /**
     * 读取指定用户的 apiKey 索引列表（该用户名下全部 Key 的值）
     *
     * @param loginId 用户 id
     * @return apiKey 值列表，无记录返回空列表
     */
    java.util.List<String> getIndexList(Object loginId);

    /**
     * 保存指定用户的 apiKey 索引列表（全量覆盖）
     *
     * @param loginId  用户 id
     * @param apiKeyList apiKey 值列表
     * @param ttlSeconds 索引有效期（秒），按该用户名下最长 Key 的有效期对齐，-1 代表永不过期
     */
    void saveIndexList(Object loginId, java.util.List<String> apiKeyList, long ttlSeconds);

    /**
     * 删除指定用户的索引记录
     *
     * @param loginId 用户 id
     */
    void deleteIndex(Object loginId);
}
