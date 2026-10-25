package com.wxl52d41.apisign.core;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * nonce 防重放存储的默认内存实现
 *
 * 惰性过期：get 时校验并移除过期项，set 时在条目数超过阈值后触发一次全量清理。
 * 无后台线程、零依赖，适合单体服务；多实例部署请自行提供 Redis 实现替换本 Bean。
 *
 * @author wxl52d41
 * @since 1.0.0
 */
public class MemoryNonceStore implements NonceStore {

    /**
     * 触发全量清理的条目数阈值
     */
    private static final int CLEANUP_THRESHOLD = 1024;

    /**
     * key -> 存储条目（值 + 过期时间戳）
     */
    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public String get(String key) {
        Entry entry = store.get(key);
        if (entry == null) {
            return null;
        }
        // 已过期则移除并视为不存在
        if (entry.expireAt < System.currentTimeMillis()) {
            store.remove(key);
            return null;
        }
        return entry.value;
    }

    @Override
    public void set(String key, String value, long ttlSeconds) {
        store.put(key, new Entry(value, System.currentTimeMillis() + ttlSeconds * 1000));
        // 超过阈值时清理一次过期项，避免长期运行内存无限增长
        if (store.size() > CLEANUP_THRESHOLD) {
            cleanup();
        }
    }

    @Override
    public boolean setIfAbsent(String key, String value, long ttlSeconds) {
        long now = System.currentTimeMillis();
        long expireAt = now + ttlSeconds * 1000;
        // 用固定哨兵值代表"本次调用新建了条目"：compute 对同一 key 是原子操作，
        // 并发下仅一个线程能看到 old == null 或已过期，从而只有它拿到 true
        Entry created = new Entry(value, expireAt);
        Entry result = store.compute(key, (k, old) -> {
            if (old == null || old.expireAt < now) {
                return created;
            }
            return old;
        });
        if (store.size() > CLEANUP_THRESHOLD) {
            cleanup();
        }
        // 引用相等判断：只有真正写入 created 的那次调用才为 true
        return result == created;
    }

    /**
     * 清理所有已过期的条目
     */
    private void cleanup() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Entry>> it = store.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().expireAt < now) {
                it.remove();
            }
        }
    }

    /**
     * 存储条目：值 + 过期时间戳
     */
    private static class Entry {

        private final String value;

        private final long expireAt;

        Entry(String value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }
}
