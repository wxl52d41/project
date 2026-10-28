package com.wxl52d41.apikey.starter.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wxl52d41.apikey.core.ApiKeyModel;
import com.wxl52d41.apikey.core.ApiKeyStore;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * ApiKeyStore 的 Redis 实现（唯一默认实现）
 *
 * 数据存于 Redis，多实例部署天然共享；
 * key 结构：
 * - 模型：api-key:model:{apiKey}
 * - 索引：api-key:index:{loginId}
 *
 * @author wxl52d41
 * @since 1.0.0
 */
public class RedisApiKeyStore implements ApiKeyStore {

    /**
     * 模型存储 key 前缀
     */
    public static final String MODEL_KEY_PREFIX = "api-key:model:";

    /**
     * 索引存储 key 前缀
     */
    public static final String INDEX_KEY_PREFIX = "api-key:index:";

    /**
     * 独立 ObjectMapper，避免宿主序列化配置影响模型读写
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final StringRedisTemplate redisTemplate;

    public RedisApiKeyStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public ApiKeyModel getApiKey(String apiKey) {
        String json = redisTemplate.opsForValue().get(MODEL_KEY_PREFIX + apiKey);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ApiKeyModel.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void saveApiKey(String apiKey, ApiKeyModel model, long ttlSeconds) {
        String json;
        try {
            json = objectMapper.writeValueAsString(model);
        } catch (Exception e) {
            throw new IllegalStateException("ApiKeyModel 序列化失败", e);
        }
        if (ttlSeconds == ApiKeyModel.NEVER_EXPIRE) {
            // 永不过期不设 TTL（与 -1 语义对齐）
            redisTemplate.opsForValue().set(MODEL_KEY_PREFIX + apiKey, json);
        } else {
            redisTemplate.opsForValue().set(MODEL_KEY_PREFIX + apiKey, json,
                    ttlSeconds, TimeUnit.SECONDS);
        }
    }

    @Override
    public void deleteApiKey(String apiKey) {
        redisTemplate.delete(MODEL_KEY_PREFIX + apiKey);
    }

    @Override
    public List<String> getIndexList(Object loginId) {
        String json = redisTemplate.opsForValue().get(INDEX_KEY_PREFIX + loginId);
        if (json == null) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public void saveIndexList(Object loginId, List<String> apiKeyList, long ttlSeconds) {
        String json;
        try {
            json = objectMapper.writeValueAsString(apiKeyList);
        } catch (Exception e) {
            throw new IllegalStateException("索引序列化失败", e);
        }
        if (ttlSeconds == ApiKeyModel.NEVER_EXPIRE) {
            redisTemplate.opsForValue().set(INDEX_KEY_PREFIX + loginId, json);
        } else {
            redisTemplate.opsForValue().set(INDEX_KEY_PREFIX + loginId, json,
                    ttlSeconds, TimeUnit.SECONDS);
        }
    }

    @Override
    public void deleteIndex(Object loginId) {
        redisTemplate.delete(INDEX_KEY_PREFIX + loginId);
    }
}
