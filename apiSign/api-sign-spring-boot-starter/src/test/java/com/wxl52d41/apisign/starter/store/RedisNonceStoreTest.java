package com.wxl52d41.apisign.starter.store;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RedisNonceStore 单测
 *
 * 不依赖真实 Redis：用 mock 验证「与 Redis 的交互是否正确的」，
 * 真实 Redis 的原子性由 SET NX EX 的语义保证（Lua 脚本，见被测类）。
 */
public class RedisNonceStoreTest {

    private StringRedisTemplate redisTemplate;

    private ValueOperations<String, String> valueOps;

    private RedisNonceStore store;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        store = new RedisNonceStore(redisTemplate);
    }

    @Test
    public void testGet() {
        when(valueOps.get("api-sign:nonce:thirdA:abc")).thenReturn("abc");
        Assertions.assertEquals("abc", store.get("api-sign:nonce:thirdA:abc"));

        when(valueOps.get("missing")).thenReturn(null);
        Assertions.assertNull(store.get("missing"));
    }

    @Test
    public void testSetWithTtlInSeconds() {
        store.set("k", "v", 1802L);
        // 必须带 TTL，且单位是秒，否则 key 永不过期导致 Redis 内存无限增长
        verify(valueOps).set(eq("k"), eq("v"), eq(1802L), eq(TimeUnit.SECONDS));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetIfAbsentReturnsTrueWhenScriptReturnsOne() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString()))
                .thenReturn(1L);

        Assertions.assertTrue(store.setIfAbsent("k", "v", 1802L));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetIfAbsentReturnsFalseWhenScriptReturnsZero() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString()))
                .thenReturn(0L);

        Assertions.assertFalse(store.setIfAbsent("k", "v", 1802L));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetIfAbsentHandlesNullResult() {
        // Redis 返回 null（如管道/事务模式）时应视为失败，不能 NPE
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString()))
                .thenReturn(null);

        Assertions.assertFalse(store.setIfAbsent("k", "v", 1802L));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetIfAbsentPassesKeyAndTtlToScript() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString()))
                .thenReturn(1L);

        store.setIfAbsent("api-sign:nonce:thirdA:abc", "abc", 1802L);

        // KEYS[1] 只有一个 key；ARGV 为 value 与 ttl（ttl 必须是字符串形式的秒数）
        verify(redisTemplate).execute(any(RedisScript.class),
                eq(java.util.Collections.singletonList("api-sign:nonce:thirdA:abc")),
                eq("abc"), eq("1802"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetIfAbsentMustNotUseNonAtomicGetSet() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString()))
                .thenReturn(1L);

        store.setIfAbsent("k", "v", 60L);

        // 防重放的安全根基：绝不能退化成 get + set 组合
        verify(valueOps, never()).get(anyString());
        verify(valueOps, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }
}
