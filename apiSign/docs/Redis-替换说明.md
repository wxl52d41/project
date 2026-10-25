# 替换 nonce 存储为 Redis

当前默认用 `MemoryNonceStore`（`ConcurrentHashMap`）。**单机可用，多实例必须换 Redis**，原因：

> `MemoryNonceStore` 的 nonce 记录只存在本实例内存里。多实例部署时，同一请求打到实例 A 消耗了 nonce，再打到实例 B 时 B 的内存里没有这条记录，**重放依然会通过**，防重放形同虚设。

---

## 一、已提供的实现

`api-sign-spring-boot-starter` 内置 `RedisNonceStore`：

```
api-sign-spring-boot-starter/src/main/java/com/wxl52d41/apisign/starter/store/RedisNonceStore.java
```

核心是 `setIfAbsent` 用 Lua 脚本实现原子 `SET NX EX`：

```lua
if redis.call('set', KEYS[1], ARGV[1], 'NX', 'EX', ARGV[2]) then return 1 else return 0 end
```

**必须是原子操作**。若写成 `if (get(k) == null) { set(k, v); return true; }`，并发下两个请求会同时读到 `null`，都返回成功，防重放失效。

---

## 二、替换步骤

### 步骤 1：引入 Redis 依赖

starter 已 `optional` 引入 `spring-boot-starter-data-redis`，宿主引入即生效：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 步骤 2：配置 Redis 连接

Boot 2.x：

```yaml
spring:
  redis:
    host: 127.0.0.1
    port: 6379
    password: # 有密码则填
    database: 0
    timeout: 3000
    lettuce:
      pool:
        max-active: 16
        max-idle: 8
        min-idle: 2
```

Boot 3.x 改前缀为 `spring.data.redis.*`。

### 步骤 3：启用 Redis Bean

打开 `ApiSignAutoConfiguration.java`，把这段的注释去掉：

```java
// @Bean
// @ConditionalOnClass({StringRedisTemplate.class})
// @ConditionalOnBean({StringRedisTemplate.class})
// @ConditionalOnMissingBean(NonceStore.class)
// public NonceStore redisNonceStore(StringRedisTemplate stringRedisTemplate) {
//     return new RedisNonceStore(stringRedisTemplate);
// }
```

同时补充 import：

```java
import com.wxl52d41.apisign.starter.store.RedisNonceStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.StringRedisTemplate;
```

三个注解的作用：

| 注解 | 作用 |
|------|------|
| `@ConditionalOnClass(StringRedisTemplate.class)` | 宿主没引 Spring Data Redis 时跳过，不报 ClassNotFound |
| `@ConditionalOnBean(StringRedisTemplate.class)` | 引了依赖但没配连接时跳过，避免注入失败 |
| `@ConditionalOnMissingBean(NonceStore.class)` | 与默认 `nonceStore()` 互斥，不会两个 Bean 冲突 |

> 原有的 `nonceStore()`（返回 `MemoryNonceStore`）**保留不动**，作为兜底。两个 Bean 都带 `@ConditionalOnMissingBean(NonceStore.class)`，Spring 只会注册其中一个。

---

## 三、不修改 starter 的替代方式

如果你不想动 starter 源码（比如它作为外部 jar 引入），在**宿主项目**里注册 Bean 即可，效果完全一样：

```java
@Configuration
public class NonceStoreConfig {

    @Bean
    public NonceStore nonceStore(StringRedisTemplate redisTemplate) {
        return new RedisNonceStore(redisTemplate);
    }
}
```

`ApiSignAutoConfiguration` 里的默认 `nonceStore()` 带 `@ConditionalOnMissingBean(NonceStore.class)`，检测到你注册了 Bean 就自动跳过。

这是**推荐方式**：starter 保持干净，业务侧掌握存储实现。

---

## 四、不想用 StringRedisTemplate 时

只要实现 `NonceStore` 接口就行，用 Jedis / Redisson / 任意客户端均可：

```java
public class MyRedisNonceStore implements NonceStore {

    private final JedisPool pool;

    @Override
    public String get(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.get(key);
        }
    }

    @Override
    public void set(String key, String value, long ttlSeconds) {
        try (Jedis jedis = pool.getResource()) {
            jedis.setex(key, ttlSeconds, value);
        }
    }

    @Override
    public boolean setIfAbsent(String key, String value, long ttlSeconds) {
        try (Jedis jedis = pool.getResource()) {
            // SET key value NX EX ttl  —— 一条命令完成，天然原子
            String r = jedis.set(key, value, SetParams.setParams().nx().ex(ttlSeconds));
            return "OK".equals(r);
        }
    }
}
```

---

## 五、注意事项

| 项 | 说明 |
|----|------|
| **必须带 TTL** | 漏掉 EX 会导致 key 永不过期，Redis 内存持续增长 |
| **TTL 必须大于时间窗** | 代码里是 `timestampDisparity * 2 + 2` 秒。若 TTL 小于时间窗，会出现「timestamp 还在窗口内、但 nonce 记录已过期」的间隙期，可被重放 |
| **不要用 get + set 组合** | 并发下两个请求同时读到 null，防重放失效 |
| **Redis 挂了怎么办** | 当前实现未做降级：Redis 不可用会抛异常，验签直接失败（返回 500 或 12202）。这是**安全优先**的选择——降级到内存等于临时关掉防重放 |
| **key 结构** | `api-sign:nonce:{appId}:{nonce}`，带 appId 用于隔离不同接入方的 nonce 空间 |
| **Redis 版本** | 2.6.12+（`SET NX EX` 需要）；低于此版本或使用不支持该参数的代理，改用 `SETNX` + `EXPIRE` 两条命令（存在极小间隙，建议升级） |
| **集群模式** | 单 key 操作，Lua 脚本只访问 `KEYS[1]`，可直接用于 Redis Cluster，不会跨槽 |

---

## 六、切换后自测

```bash
# 1. 发起一次请求，确认 key 写入
redis-cli keys "api-sign:nonce:*"

# 2. 确认有 TTL（应接近 nonce TTL，不是 -1）
redis-cli ttl "api-sign:nonce:thirdA:你的nonce值"

# 3. 用同一 nonce 重放，应返回 12202
curl -X POST ... -H "nonce: 同一个值" ...
```

多实例验证：请求实例 A 后，立刻用同一 nonce 请求实例 B，应同样返回 12202。这是换 Redis 的核心目的，务必验证。
