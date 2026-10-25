package com.wxl52d41.apisign.core;

/**
 * nonce 防重放存储接口
 *
 * 替代 sa-token 的 SaTokenDao 职责（本模块仅用到 get/set 两个能力），
 * 默认提供内存实现 {@link MemoryNonceStore}，多实例部署时请自行提供 Redis 实现，例如：
 * 
 * &#64;Bean
 * public NonceStore nonceStore(RedisTemplate&lt;String, String&gt; redisTemplate) {
 *     return new NonceStore() {
 *         &#64;Override
 *         public String get(String key) {
 *             return redisTemplate.opsForValue().get(key);
 *         }
 *         &#64;Override
 *         public void set(String key, String value, long ttlSeconds) {
 *             redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
 *         }
 *     };
 * }
 * 
 *
 * @author wxl52d41
 * @since 1.0.0
 */
public interface NonceStore {

    /**
     * 根据 key 获取值，不存在或已过期时返回 null
     *
     * @param key key
     * @return value
     */
    String get(String key);

    /**
     * 写入值并设置过期时间（单位：秒）
     *
     * @param key        key
     * @param value      value
     * @param ttlSeconds 过期时间（秒）
     */
    void set(String key, String value, long ttlSeconds);

    /**
     * 原子写入：仅当 key 不存在（或已过期）时写入，返回是否写入成功。
     *
     * 用于 nonce 防重放，必须保证并发下同一 nonce 只有一个请求能写入成功；
     * 自定义实现请使用原子的 SETNX / SET NX EX 语义，不要用 get + set 组合（存在并发漏洞）。
     *
     * @param key        key
     * @param value      value
     * @param ttlSeconds 过期时间（秒）
     * @return 是否写入成功（true 代表此前不存在）
     */
    boolean setIfAbsent(String key, String value, long ttlSeconds);
}
