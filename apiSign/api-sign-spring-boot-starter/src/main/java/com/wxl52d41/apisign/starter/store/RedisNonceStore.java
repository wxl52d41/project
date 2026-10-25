package com.wxl52d41.apisign.starter.store;

import com.wxl52d41.apisign.core.NonceStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;

/**
 * nonce 防重放存储的 Redis 实现
 *
 * 多实例部署时必须使用本实现：内存实现 {@code MemoryNonceStore} 的 nonce 空间按实例隔离，
 * 同一请求打到另一实例即可重放成功，等于没有防重放能力。
 *
 * setIfAbsent 使用 Lua 脚本实现原子的 SET NX EX，保证并发下同一 nonce 只有一个请求能写入成功。
 * 不直接用 {@code opsForValue().setIfAbsent(k, v, ttl, unit)} 的原因：该方法要求 Redis 2.6.12+，
 * 且部分代理（如 Codis、Twemproxy）对 SET 带 NX EX 参数的兼容性不一致，Lua 方式更通用稳定。
 *
 * @author wxl52d41
 * @since 1.0.0
 */
public class RedisNonceStore implements NonceStore {

    /**
     * 原子写入脚本：key 不存在时写入并设置过期时间，返回 1；已存在返回 0
     *
     * KEYS[1] = key，ARGV[1] = value，ARGV[2] = ttl 秒
     */
    private static final RedisScript<Long> SET_NX_EX_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('set', KEYS[1], ARGV[1], 'NX', 'EX', ARGV[2]) then return 1 else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisNonceStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void set(String key, String value, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, value, ttlSeconds, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Override
    public boolean setIfAbsent(String key, String value, long ttlSeconds) {
        Long result = redisTemplate.execute(SET_NX_EX_SCRIPT,
                Collections.singletonList(key), value, String.valueOf(ttlSeconds));
        return result != null && result == 1L;
    }
}
