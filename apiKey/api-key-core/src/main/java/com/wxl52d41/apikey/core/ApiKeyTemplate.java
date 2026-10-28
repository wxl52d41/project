package com.wxl52d41.apikey.core;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * API Key 操作类
 *
 * 拷贝自 sa-token 的 SaApiKeyTemplate，做无 sa-token 化改造：
 * - SaTokenDao + SaSession 存储组合  → ApiKeyStore 接口
 * - SaManager.getConfig()             → 构造注入 ApiKeyConfig
 * - SaStrategy.generateUniqueToken    → 本地重试生成（存在性检查防碰撞）
 * - SaFoxUtil.getRandomString         → SecureRandom
 * - RawSession 索引                    → store.getIndexList / saveIndexList
 *
 * 流程：createApiKeyModel 创建 → setLoginId 等补全 → saveApiKey 持久化（含索引维护）
 *
 * @author click33 / wxl52d41
 * @since 1.0.0
 */
public class ApiKeyTemplate {

    /**
     * 随机数来源（ApiKey 值的生成必须不可预测）
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * ApiKey 值的随机字符表
     */
    private static final String CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    /**
     * ApiKey 存储
     */
    private final ApiKeyStore apiKeyStore;

    /**
     * 配置
     */
    private final ApiKeyConfig config;

    public ApiKeyTemplate(ApiKeyStore apiKeyStore, ApiKeyConfig config) {
        this.apiKeyStore = apiKeyStore;
        this.config = config == null ? new ApiKeyConfig() : config;
    }

    /**
     * 获取 ApiKeyModel，无效的 ApiKey 会返回 null
     *
     * @param apiKey apiKey
     * @return 模型
     */
    public ApiKeyModel getApiKey(String apiKey) {
        if (apiKey == null) {
            return null;
        }
        return apiKeyStore.getApiKey(apiKey);
    }

    /**
     * 校验 ApiKey，成功返回 ApiKeyModel，失败则抛出异常
     *
     * @param apiKey apiKey
     * @return 模型
     */
    public ApiKeyModel checkApiKey(String apiKey) {
        ApiKeyModel ak = getApiKey(apiKey);
        if (ak == null) {
            throw new ApiKeyException("无效 API Key: " + apiKey).setApiKey(apiKey).setCode(ApiKeyErrorCode.CODE_12301);
        }
        if (ak.timeExpired()) {
            throw new ApiKeyException("API Key 已过期: " + apiKey).setApiKey(apiKey).setCode(ApiKeyErrorCode.CODE_12302);
        }
        if (!ak.getIsValid()) {
            throw new ApiKeyException("API Key 已被禁用: " + apiKey).setApiKey(apiKey).setCode(ApiKeyErrorCode.CODE_12303);
        }
        return ak;
    }

    /**
     * 持久化：ApiKeyModel（含索引维护）
     *
     * @param ak 模型
     */
    public void saveApiKey(ApiKeyModel ak) {
        if (ak == null) {
            return;
        }
        // 数据自检
        ak.checkByCanSaved();

        // 已过期的 Key 直接删除（存储层 TTL 双保险）
        if (ak.timeExpired()) {
            apiKeyStore.deleteApiKey(ak.getApiKey());
        } else {
            apiKeyStore.saveApiKey(ak.getApiKey(), ak, ak.expiresIn());
        }

        // 记录索引
        if (getIsRecordIndex()) {
            saveIndex(ak);
        }
    }

    /**
     * 维护索引：把本 Key 并入用户的索引列表，并把索引 TTL 对齐到名下最长 Key 的有效期
     *
     * @param ak 模型
     */
    private void saveIndex(ApiKeyModel ak) {
        List<String> apiKeyList = apiKeyStore.getIndexList(ak.getLoginId());
        if (!apiKeyList.contains(ak.getApiKey())) {
            apiKeyList.add(ak.getApiKey());
        }
        apiKeyStore.saveIndexList(ak.getLoginId(), apiKeyList, calcIndexTtl(apiKeyList));
    }

    /**
     * 计算：索引记录的有效期应设为多长（名下所有有效 Key 的最长剩余有效期）
     *
     * 索引 TTL 若短于 Key 的有效期，会出现「Key 还在但索引已消失」导致 getApiKeyList 漏查；
     * 略长无害——下次 saveApiKey / adjustIndex 时会重新对齐
     *
     * @param apiKeyList 该用户名下全部 Key
     * @return 秒，-1 代表永不过期
     */
    private long calcIndexTtl(List<String> apiKeyList) {
        long maxTtl = 0;
        for (String apiKey : apiKeyList) {
            ApiKeyModel ak = apiKeyStore.getApiKey(apiKey);
            if (ak == null || ak.timeExpired()) {
                continue;
            }
            long ttl = ak.expiresIn();
            if (ttl == ApiKeyModel.NEVER_EXPIRE) {
                return ApiKeyModel.NEVER_EXPIRE;
            }
            if (ttl > maxTtl) {
                maxTtl = ttl;
            }
        }
        return maxTtl;
    }

    /**
     * 获取 ApiKey 所代表的 LoginId
     *
     * @param apiKey apiKey
     * @return loginId
     */
    public Object getLoginIdByApiKey(String apiKey) {
        return checkApiKey(apiKey).getLoginId();
    }

    /**
     * 删除 ApiKey（含索引维护）
     *
     * @param apiKey apiKey
     */
    public void deleteApiKey(String apiKey) {
        ApiKeyModel ak = apiKeyStore.getApiKey(apiKey);
        if (ak == null) {
            return;
        }
        apiKeyStore.deleteApiKey(apiKey);
        if (getIsRecordIndex()) {
            List<String> apiKeyList = apiKeyStore.getIndexList(ak.getLoginId());
            apiKeyList.remove(apiKey);
            if (apiKeyList.isEmpty()) {
                apiKeyStore.deleteIndex(ak.getLoginId());
            } else {
                apiKeyStore.saveIndexList(ak.getLoginId(), apiKeyList, calcIndexTtl(apiKeyList));
            }
        }
    }

    /**
     * 删除指定 loginId 的所有 ApiKey
     *
     * @param loginId 用户 id
     */
    public void deleteApiKeyByLoginId(Object loginId) {
        if (!getIsRecordIndex()) {
            throw new ApiKeyException("当前 API Key 模块未开启索引记录功能，无法执行 deleteApiKeyByLoginId 操作")
                    .setCode(ApiKeyErrorCode.CODE_12305);
        }
        List<String> apiKeyList = apiKeyStore.getIndexList(loginId);
        for (String apiKey : apiKeyList) {
            apiKeyStore.deleteApiKey(apiKey);
        }
        apiKeyStore.deleteIndex(loginId);
    }

    // ------- 创建

    /**
     * 创建一个 ApiKeyModel 对象（不持久化，补全信息后调 saveApiKey 保存）
     *
     * @return 模型
     */
    public ApiKeyModel createApiKeyModel() {
        // 重试生成直到不与存量碰撞
        String apiKey = randomApiKeyValue();
        for (int i = 0; i < 100 && apiKeyStore.getApiKey(apiKey) != null; i++) {
            apiKey = randomApiKeyValue();
        }
        return new ApiKeyModel().setApiKey(apiKey);
    }

    /**
     * 创建一个补全了有效期与状态的 ApiKeyModel 对象
     *
     * @param loginId 用户 id
     * @return 模型
     */
    public ApiKeyModel createApiKeyModel(Object loginId) {
        long timeout = config.getTimeout();
        long expiresTime = (timeout == ApiKeyModel.NEVER_EXPIRE)
                ? ApiKeyModel.NEVER_EXPIRE
                : System.currentTimeMillis() + timeout * 1000;
        return createApiKeyModel()
                .setLoginId(loginId)
                .setIsValid(true)
                .setExpiresTime(expiresTime);
    }

    /**
     * 随机一个 ApiKey 码：前缀 + 36 位随机串
     *
     * @return apiKey 值
     */
    public String randomApiKeyValue() {
        return config.getPrefix() + getRandomString(36);
    }

    // ------------------- 校验

    /**
     * 判断：指定 ApiKey 是否具有指定 Scope 列表 (AND 模式，需要全部具备)，返回 true 或 false
     *
     * @param apiKey apiKey
     * @param scopes 需要校验的权限列表
     * @return 是否具备
     */
    public boolean hasApiKeyScope(String apiKey, String... scopes) {
        try {
            checkApiKeyScope(apiKey, scopes);
            return true;
        } catch (ApiKeyException e) {
            return false;
        }
    }

    /**
     * 校验：指定 ApiKey 是否具有指定 Scope 列表 (AND 模式，需要全部具备)，如果不具备则抛出异常
     *
     * @param apiKey apiKey
     * @param scopes 需要校验的权限列表
     */
    public void checkApiKeyScope(String apiKey, String... scopes) {
        ApiKeyModel ak = checkApiKey(apiKey);
        if (scopes == null || scopes.length == 0) {
            return;
        }
        for (String scope : scopes) {
            if (!ak.getScopes().contains(scope)) {
                throw new ApiKeyScopeException("该 API Key 不具备 Scope：" + scope)
                        .setApiKey(apiKey)
                        .setScope(scope)
                        .setCode(ApiKeyErrorCode.CODE_12311);
            }
        }
    }

    /**
     * 判断：指定 ApiKey 是否具有指定 Scope 列表 (OR 模式，具备其一即可)，返回 true 或 false
     *
     * @param apiKey apiKey
     * @param scopes 需要校验的权限列表
     * @return 是否具备
     */
    public boolean hasApiKeyScopeOr(String apiKey, String... scopes) {
        try {
            checkApiKeyScopeOr(apiKey, scopes);
            return true;
        } catch (ApiKeyException e) {
            return false;
        }
    }

    /**
     * 校验：指定 ApiKey 是否具有指定 Scope 列表 (OR 模式，具备其一即可)，如果不具备则抛出异常
     *
     * @param apiKey apiKey
     * @param scopes 需要校验的权限列表
     */
    public void checkApiKeyScopeOr(String apiKey, String... scopes) {
        ApiKeyModel ak = checkApiKey(apiKey);
        if (scopes == null || scopes.length == 0) {
            return;
        }
        for (String scope : scopes) {
            if (ak.getScopes().contains(scope)) {
                return;
            }
        }
        throw new ApiKeyScopeException("该 API Key 不具备 Scope：" + scopes[0])
                .setApiKey(apiKey)
                .setScope(scopes[0])
                .setCode(ApiKeyErrorCode.CODE_12311);
    }

    /**
     * 判断：指定 ApiKey 是否属于指定 LoginId，返回 true 或 false
     *
     * @param apiKey apiKey
     * @param loginId 用户 id
     * @return 是否属于
     */
    public boolean isApiKeyLoginId(String apiKey, Object loginId) {
        try {
            checkApiKeyLoginId(apiKey, loginId);
            return true;
        } catch (ApiKeyException e) {
            return false;
        }
    }

    /**
     * 校验：指定 ApiKey 是否属于指定 LoginId，如果不是则抛出异常
     *
     * @param apiKey apiKey
     * @param loginId 用户 id
     */
    public void checkApiKeyLoginId(String apiKey, Object loginId) {
        ApiKeyModel ak = getApiKey(apiKey);
        if (ak == null) {
            throw new ApiKeyException("无效 API Key: " + apiKey).setApiKey(apiKey).setCode(ApiKeyErrorCode.CODE_12301);
        }
        if (!String.valueOf(ak.getLoginId()).equals(String.valueOf(loginId))) {
            throw new ApiKeyException("该 API Key 不属于用户: " + loginId)
                    .setApiKey(apiKey)
                    .setCode(ApiKeyErrorCode.CODE_12312);
        }
    }

    // ------------------- 索引操作

    /**
     * 整理索引：清理过期 Key 的索引残留，并把索引 TTL 对齐到名下最长 Key 的有效期
     *
     * @param loginId 用户 id
     */
    public void adjustIndex(Object loginId) {
        if (!getIsRecordIndex()) {
            return;
        }
        List<String> apiKeyList = apiKeyStore.getIndexList(loginId);
        List<String> newApiKeyList = new ArrayList<>();
        for (String apiKey : apiKeyList) {
            ApiKeyModel ak = apiKeyStore.getApiKey(apiKey);
            if (ak != null && !ak.timeExpired()) {
                newApiKeyList.add(apiKey);
            }
        }
        if (newApiKeyList.isEmpty()) {
            apiKeyStore.deleteIndex(loginId);
            return;
        }
        apiKeyStore.saveIndexList(loginId, newApiKeyList, calcIndexTtl(newApiKeyList));
    }

    /**
     * 获取指定 loginId 的 ApiKey 列表记录
     *
     * @param loginId 用户 id
     * @return 模型列表
     */
    public List<ApiKeyModel> getApiKeyList(Object loginId) {
        if (!getIsRecordIndex()) {
            return new ArrayList<>();
        }
        List<ApiKeyModel> apiKeyModelList = new ArrayList<>();
        for (String apiKey : apiKeyStore.getIndexList(loginId)) {
            ApiKeyModel ak = apiKeyStore.getApiKey(apiKey);
            if (ak != null && !ak.timeExpired()) {
                apiKeyModelList.add(ak);
            }
        }
        return apiKeyModelList;
    }

    /**
     * 是否记录索引
     *
     * @return 是否
     */
    public boolean getIsRecordIndex() {
        return Boolean.TRUE.equals(config.getIsRecordIndex());
    }

    /**
     * 获取配置（调试 / 启动打印用）
     *
     * @return 配置对象
     */
    public ApiKeyConfig getConfig() {
        return config;
    }

    /**
     * 生成密码学安全的随机字符串
     *
     * @param length 长度
     * @return 随机串
     */
    private static String getRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(SECURE_RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
