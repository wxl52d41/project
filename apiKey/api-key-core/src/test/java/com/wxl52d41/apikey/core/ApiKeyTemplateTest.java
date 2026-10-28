package com.wxl52d41.apikey.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * ApiKeyTemplate 核心行为测试
 *
 * 用内存 Store（测试用途的轻量实现）验证：
 * 创建/保存/查询/校验/Scope/归属/过期/禁用/索引维护/批量删除
 */
public class ApiKeyTemplateTest {

    private ApiKeyTemplate template;

    /**
     * 测试用轻量内存 Store（core 无 Jackson 依赖，直接存对象引用）
     */
    static class SimpleStore implements ApiKeyStore {

        final java.util.Map<String, ApiKeyModel> apiKeyMap = new java.util.concurrent.ConcurrentHashMap<>();
        final java.util.Map<String, List<String>> indexMap = new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public ApiKeyModel getApiKey(String apiKey) {
            return apiKeyMap.get(apiKey);
        }

        @Override
        public void saveApiKey(String apiKey, ApiKeyModel model, long ttlSeconds) {
            apiKeyMap.put(apiKey, model);
        }

        @Override
        public void deleteApiKey(String apiKey) {
            apiKeyMap.remove(apiKey);
        }

        @Override
        public List<String> getIndexList(Object loginId) {
            List<String> list = indexMap.get(String.valueOf(loginId));
            return list == null ? new ArrayList<>() : new ArrayList<>(list);
        }

        @Override
        public void saveIndexList(Object loginId, List<String> apiKeyList, long ttlSeconds) {
            indexMap.put(String.valueOf(loginId), new ArrayList<>(apiKeyList));
        }

        @Override
        public void deleteIndex(Object loginId) {
            indexMap.remove(String.valueOf(loginId));
        }
    }

    @BeforeEach
    public void setUp() {
        template = new ApiKeyTemplate(new SimpleStore(), new ApiKeyConfig());
    }

    // 创建：前缀 + 36 位随机串
    @Test
    public void testCreateApiKeyModel() {
        ApiKeyModel ak = template.createApiKeyModel(10001);
        Assertions.assertTrue(ak.getApiKey().startsWith("AK-"));
        Assertions.assertEquals(39, ak.getApiKey().length());  // AK- + 36
        Assertions.assertEquals(10001, ak.getLoginId());
        Assertions.assertTrue(ak.getIsValid());
        Assertions.assertTrue(ak.getExpiresTime() > System.currentTimeMillis());
    }

    // 保存后可查询，索引同步记录
    @Test
    public void testSaveAndList() {
        ApiKeyModel ak = template.createApiKeyModel(10001);
        template.saveApiKey(ak);

        Assertions.assertNotNull(template.getApiKey(ak.getApiKey()));
        List<ApiKeyModel> list = template.getApiKeyList(10001);
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals(ak.getApiKey(), list.get(0).getApiKey());
    }

    // 校验：无效 Key 12301
    @Test
    public void testCheckInvalid() {
        ApiKeyException e = Assertions.assertThrows(ApiKeyException.class,
                () -> template.checkApiKey("AK-not-exists"));
        Assertions.assertEquals(ApiKeyErrorCode.CODE_12301, e.getCode());
    }

    // 校验：过期 Key 12302
    @Test
    public void testCheckExpired() {
        // saveApiKey 对已过期 Key 会直接删除（对齐 sa-token），故先存有效 Key 再改存储模拟自然过期
        ApiKeyModel ak = template.createApiKeyModel(10001);
        template.saveApiKey(ak);
        ak.setExpiresTime(System.currentTimeMillis() - 1000);
        ApiKeyException e = Assertions.assertThrows(ApiKeyException.class,
                () -> template.checkApiKey(ak.getApiKey()));
        Assertions.assertEquals(ApiKeyErrorCode.CODE_12302, e.getCode());
    }

    // 校验：禁用 Key 12303
    @Test
    public void testCheckDisabled() {
        ApiKeyModel ak = template.createApiKeyModel(10001).setIsValid(false);
        template.saveApiKey(ak);
        ApiKeyException e = Assertions.assertThrows(ApiKeyException.class,
                () -> template.checkApiKey(ak.getApiKey()));
        Assertions.assertEquals(ApiKeyErrorCode.CODE_12303, e.getCode());
    }

    // Scope：AND 模式全部具备才通过
    @Test
    public void testScopeAnd() {
        ApiKeyModel ak = template.createApiKeyModel(10001).addScope("userinfo", "chat");
        template.saveApiKey(ak);

        Assertions.assertTrue(template.hasApiKeyScope(ak.getApiKey(), "userinfo", "chat"));
        Assertions.assertFalse(template.hasApiKeyScope(ak.getApiKey(), "userinfo", "pull"));

        ApiKeyException e = Assertions.assertThrows(ApiKeyException.class,
                () -> template.checkApiKeyScope(ak.getApiKey(), "userinfo", "pull"));
        Assertions.assertEquals(ApiKeyErrorCode.CODE_12311, e.getCode());
    }

    // Scope：OR 模式具备其一即通过
    @Test
    public void testScopeOr() {
        ApiKeyModel ak = template.createApiKeyModel(10001).addScope("userinfo");
        template.saveApiKey(ak);

        Assertions.assertTrue(template.hasApiKeyScopeOr(ak.getApiKey(), "userinfo", "chat"));
        Assertions.assertFalse(template.hasApiKeyScopeOr(ak.getApiKey(), "pull", "commit"));
    }

    // 归属校验：他人的 Key 12312
    @Test
    public void testLoginIdCheck() {
        ApiKeyModel ak = template.createApiKeyModel(10001);
        template.saveApiKey(ak);

        Assertions.assertTrue(template.isApiKeyLoginId(ak.getApiKey(), 10001));
        // loginId 类型不同但值相同（10001 vs "10001"）视为同一用户，与 sa-token 口径一致
        Assertions.assertTrue(template.isApiKeyLoginId(ak.getApiKey(), "10001"));
        Assertions.assertFalse(template.isApiKeyLoginId(ak.getApiKey(), 10002));
    }

    // getLoginIdByApiKey：校验通过返回归属用户
    @Test
    public void testGetLoginIdByApiKey() {
        ApiKeyModel ak = template.createApiKeyModel(10001);
        template.saveApiKey(ak);
        Assertions.assertEquals(10001, template.getLoginIdByApiKey(ak.getApiKey()));
    }

    // 删除单个：模型与索引同步清理
    @Test
    public void testDeleteOne() {
        ApiKeyModel ak = template.createApiKeyModel(10001);
        template.saveApiKey(ak);
        template.deleteApiKey(ak.getApiKey());

        Assertions.assertNull(template.getApiKey(ak.getApiKey()));
        Assertions.assertEquals(0, template.getApiKeyList(10001).size());
    }

    // 批量删除：按用户清空
    @Test
    public void testDeleteByLoginId() {
        template.saveApiKey(template.createApiKeyModel(10001));
        template.saveApiKey(template.createApiKeyModel(10001));
        template.saveApiKey(template.createApiKeyModel(10002));

        template.deleteApiKeyByLoginId(10001);
        Assertions.assertEquals(0, template.getApiKeyList(10001).size());
        Assertions.assertEquals(1, template.getApiKeyList(10002).size());
    }

    // adjustIndex：清掉过期 Key 的索引残留
    @Test
    public void testAdjustIndex() {
        ApiKeyModel valid = template.createApiKeyModel(10001);
        template.saveApiKey(valid);
        ApiKeyModel expired = template.createApiKeyModel(10001)
                .setExpiresTime(System.currentTimeMillis() - 1000);
        template.saveApiKey(expired);

        template.adjustIndex(10001);
        List<ApiKeyModel> list = template.getApiKeyList(10001);
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals(valid.getApiKey(), list.get(0).getApiKey());
    }

    // 唯一性：生成的 Key 不与存量碰撞（前缀不同 / 长度固定）
    @Test
    public void testKeyFormat() {
        ApiKeyModel ak1 = template.createApiKeyModel();
        ApiKeyModel ak2 = template.createApiKeyModel();
        Assertions.assertNotEquals(ak1.getApiKey(), ak2.getApiKey());
    }

    // 自定义前缀
    @Test
    public void testCustomPrefix() {
        ApiKeyConfig config = new ApiKeyConfig().setPrefix("sk-");
        ApiKeyTemplate t = new ApiKeyTemplate(new SimpleStore(), config);
        ApiKeyModel ak = t.createApiKeyModel(10001);
        Assertions.assertTrue(ak.getApiKey().startsWith("sk-"));
    }

    // 永不过期配置：-1
    @Test
    public void testNeverExpire() {
        ApiKeyConfig config = new ApiKeyConfig().setTimeout(-1);
        ApiKeyTemplate t = new ApiKeyTemplate(new SimpleStore(), config);
        ApiKeyModel ak = t.createApiKeyModel(10001);
        Assertions.assertEquals(ApiKeyModel.NEVER_EXPIRE, ak.getExpiresTime());
        Assertions.assertFalse(ak.timeExpired());
        Assertions.assertEquals(ApiKeyModel.NEVER_EXPIRE, ak.expiresIn());
    }

    // 保存自检：缺字段拒绝 12304
    @Test
    public void testCheckByCanSaved() {
        ApiKeyException e = Assertions.assertThrows(ApiKeyException.class,
                () -> template.saveApiKey(new ApiKeyModel()));
        Assertions.assertEquals(ApiKeyErrorCode.CODE_12304, e.getCode());
    }
}
