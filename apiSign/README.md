# apiSign — API 参数签名验签组件

参考 sa-token-sign 源码与社区示例 [sa-token-check-sign-from-body](https://github.com/ly-chn/sa-token-check-sign-from-body) 实现的签名验签组件，
**同时支持 query/form 参数模式与 JSON body 模式**，签名算法与 sa-token-sign 完全一致（有单测锚点）。

采用多模块结构，**服务方引入 starter 即自动生效，调用方只需 core 加签**。

## 模块结构

| 模块 | 用途 | 依赖方 |
|------|------|--------|
| `api-sign-core` | 签名算法 + `SignClient` 加签器，**零 Spring 依赖** | 服务方、第三方调用方 |
| `api-sign-spring-boot-starter` | 自动装配 `@ApiSign` 验签、多 appId 管理 | **服务方**（被调用方） |
| `api-sign-sample-spring-boot-starter` | 服务端示例 + 调试页 `/debug.html` | 参考用 |
| `api-sign-client-sample` | 第三方客户端调用示例 | 参考用（给第三方看） |

**接入方向判断**：
- 我方提供接口、对方调用 → 我方引入 **starter**，对方引入 **core** 加签
- 详见 [第三方接入指南](docs/第三方接入指南.md)

## 快速开始（服务方）

```xml
<dependency>
    <groupId>com.wxl52d41</groupId>
    <artifactId>api-sign-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

无需任何 `@EnableXxx`，引入即生效。

```yaml
api:
  sign:
    enabled: true
    app-id-header: X-Sign-App-Id
    apps:
      - app-id: thirdA
        secret-key: ******            # 建议从配置中心/环境变量注入
        digest-algo: hmac-sha256
        timestamp-disparity: 300000
        enabled: true
      - app-id: thirdB
        secret-key: ******
        digest-algo: md5
        enabled: true
```

```java
@PostMapping("order")
@ApiSign                                    // 任意已登记 appId 均可调用
public Result create(@RequestBody OrderDTO dto) { ... }

@PostMapping("query")
@ApiSign(appIds = {"thirdA"})               // 仅 thirdA 可调用
public Result query(@RequestBody Map<String, Object> body) { ... }

@PostMapping("pay")
@ApiSign(includeParams = {"orderNo", "amount"})   // 仅指定字段参与签名
public Result pay(@RequestBody Map<String, Object> body) { ... }
```

**密钥来源扩展**：默认读 YAML。需要从数据库/配置中心加载时，注册自定义 `AppSecretProvider` Bean 即可覆盖（自带 `@ConditionalOnMissingBean`）。

**多实例部署**：`NonceStore` 默认内存实现，多实例必须替换为 Redis 实现，且 `setIfAbsent` 需使用 `SET NX EX` 语义。

## 两种验签模式

验签模式由请求 `Content-Type` 自动决定：

| 模式 | 判定条件 | 业务参数来源 | timestamp / nonce / sign 来源 |
|------|---------|------------|------------------------------|
| 参数模式 | Content-Type 非 application/json | query / form 参数 | query / form 参数 |
| JSON body 模式 | Content-Type = application/json | JSON body（嵌套点号展平） | 请求 header |

两种模式下签名口径一致：`全部参与参数（含 timestamp、nonce，不含 sign）` 按字典序拼接为 `k1=v1&k2=v2`，
非 HMAC 算法再拼 `&key={secretKey}`。

## 快速开始

```yaml
api:
  sign:
    secret-key: SwqFmsKxcbq23      # 必填
    timestamp-disparity: 900000    # 时间窗（ms），-1 不校验，默认 15 分钟
    digest-algo: md5               # md5/sha1/sha256/sha384/sha512/hmac-md5/hmac-sha256/hmac-sha512
```

```java
@PostMapping("order")
@ApiSign                                        // 全部参数参与签名
public Result create(@RequestBody OrderDTO dto) { ... }

@PostMapping("query")
@ApiSign(includeParams = {"name", "age"})       // 白名单：仅指定参数参与签名
public Result query(@RequestBody Map<String, Object> body) { ... }
```

启动后有三种联调方式：

| 方式 | 地址 / 文件 | 说明 |
|------|------------|------|
| **可视化调试页** | **http://localhost:8080/debug.html** | 浏览器直接打开，自动生成签名并发送请求（推荐） |
| 签名示例接口 | `GET /demo/create-demo` | 返回各模式的签名三件套与可复制的请求文本 |
| 联调脚本 | `http/test.http` | IDEA / VS Code HTTP Client 直接运行 |

## 可视化调试页 /debug.html

随服务启动，浏览器打开 **http://localhost:8080/debug.html** 即可调试，无需手工算签名。

**功能：**

- **三种主题**：右上角「白色 / 深色 / 暖色」切换，选择保存在 localStorage，刷新不丢
- **两种入参模式自动切换**：
  - 选 `/demo/form-body` → 显示**键值对编辑器**，逐行填写参数名 / 参数值，可增删行
  - 选 JSON 接口 → 显示 JSON 文本框，嵌套对象自动按 `a.b` 展平
- **算法下拉**：`md5` / `sha256` / `hmac-md5` / `hmac-sha256`，**必须与服务端 `api.sign.digest-algo` 一致**
- **三件套自动生成**：timestamp / nonce / sign 随参数变化实时重算，并展示参与签名的参数字符串与摘要输入，方便对照排查
- **一键发送**：结果区显示 HTTP 状态码、耗时、响应体
- **请求可导出**：一键复制为 `curl` 或 `.http` 文件内容
- **内置算法自检**：页面加载即用固定向量校验 MD5 / SHA256 / HMAC 实现，并与 sa-token 官方锚点（`6f5e844a…`）比对，显示「算法自检通过」才说明前端算法与服务端同口径

**调试要点：**

- 算法下拉与服务端配置不一致必然 `401 无效签名`，这是设计行为
- `nonce` 一次性，同一个 nonce 重复发送会返回 `12202 此 nonce 已被使用过`
- 页面内展示的 sign 与实际发出的 sign 始终一致（每次发送只计算一次）

页面文件位置：`src/main/resources/static/debug.html`，随 jar 一起打包，移植时可按需保留或删除。

## 移植到其他项目

1. 拷贝 `com.wxl52d41.apisign` 包下除 `demo` 外的全部类（core / web / annotation / config 四个包，共 12 个类）；
2. 任意 `@Configuration` 上追加 `@Import(ApiSignConfiguration.class)`；
3. yml 配置 `api.sign.secret-key`（建议通过环境变量注入）；
4. （多实例部署）自行提供 `NonceStore` 实现：本模块的默认内存实现带 `@ConditionalOnMissingBean`，
   你只需在项目的配置类里定义自己的 `NonceStore` Bean 即可自动覆盖。注意 `setIfAbsent` 必须使用
   Redis 的 `SET NX EX` 语义，不要用 get + set 组合（并发下防重放会失效）。

```java
@Bean
public NonceStore nonceStore(StringRedisTemplate redisTemplate) {
    return new NonceStore() {
        @Override
        public String get(String key) {
            return redisTemplate.opsForValue().get(key);
        }
        @Override
        public void set(String key, String value, long ttlSeconds) {
            redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
        }
        @Override
        public boolean setIfAbsent(String key, String value, long ttlSeconds) {
            return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, ttlSeconds, TimeUnit.SECONDS));
        }
    };
}
```

## 客户端签名（Java）

```java
// 参数模式：业务参数 + 三件套全部放 query/form
Map<String, Object> params = new HashMap<>();
params.put("name", "nzxy");
params.put("age", 18);
signTemplate.addSignParams(params);   // 自动追加 timestamp/nonce/sign
// 把 params 全部作为表单/查询参数提交

// JSON body 模式：业务参数展平后补三件套，body 原样提交，三件套放 header
Map<String, Object> body = new HashMap<>();
body.put("user", Map.of("name", "nzxy", "age", 18));   // 嵌套结构自动展平为 user.name / user.age
Map<String, Object> signMap = new HashMap<>();
SignParamResolver.flattenObject(body, "", signMap);    // 展平
signTemplate.addSignParams(signMap);                   // 追加三件套
// header: timestamp / nonce / sign；body 原样 JSON 提交
```

## JSON 展平规则（JSON body 模式）

- 嵌套对象点号连接：`{"user":{"name":"x"}}` → `user.name=x`
- 数组用下标：`{"tags":["a","b"]}` → `tags[0]=a`、`tags[1]=b`
- null 值与空字符串不参与签名（与 sa-token 官方一致）
- 数字/布尔按文本值参与：`18`→`"18"`、`1.5`→`"1.5"`、`true`→`"true"`；注意客户端浮点序列化口径需一致（`18.0` 与 `18` 不同）

## 与 sa-token 的关系

| 本模块 | 来源 | 差异 |
|--------|------|------|
| SignTemplate | sa-token SaSignTemplate | 算法逐行拷贝；nonce 存储由 SaTokenDao 换成 NonceStore 抽象；nonce 校验改为原子写入（防并发绕过）；timestamp 格式非法转签名异常（原版会抛 NumberFormatException 变 500） |
| SignProperties | sa-token SaSignConfig | 加 @ConfigurationProperties |
| SignException / SignErrorCode | sa-token 同名类 | 错误码保持一致 |
| DigestUtils / SignUtil | sa-token SaSecureUtil / SaFoxUtil | 最小集；md5 统一 UTF-8（sa-token 用平台默认编码） |
| @ApiSign + ApiSignInterceptor | @SaCheckSign + SaCheckSignHandler | 注解处理器换成拦截器，无注册时序问题 |
| RepeatableReadRequestWrapper + Filter | 社区示例 | javax 版本；仅 json 请求包装；修复 isFinished 语义；增加 2MB body 上限与展平深度上限（防 StackOverflow / 内存耗尽） |

相对社区示例的四处修正：
1. JSON 模式下 timestamp / nonce 并入签名参数集（示例遗漏，签名未绑定时间戳和 nonce）；
2. `isFinished()` / `isReady()` 修正为真实语义；
3. body 解析异常转 `SignException`（错误码 12206），不裸抛 RuntimeException；
4. 增加 body 大小与 JSON 嵌套深度上限、timestamp 格式校验，避免恶意请求导致 500 / StackOverflow / 内存耗尽。

## 错误码

| code | 含义 | HTTP（demo 处理器） |
|------|------|--------------------|
| 12201 | 签名秘钥未配置 | 401 |
| 12202 | 签名无效 / nonce 重放 | 401 |
| 12203 | timestamp 超出时间窗 | 401 |
| 12204 | 缺少 timestamp / nonce / sign | 400 |
| 12205 | 不支持的摘要算法 | 401 |
| 12206 | 请求体读取/解析失败 | 400 |

## 测试

`mvn test` 共 35 个用例，全部通过。

| 测试类 | 覆盖内容 |
|--------|---------|
| `SignTemplateTest` | 算法一致性（期望值取自 sa-token 官方单测，证明双向互通）、时间窗、防重放、异常码 |
| `SignParamResolverTest` | JSON 展平规则、两种模式参数提取、白名单过滤、客户端/服务端展平口径一致性 |
| `SignRobustnessTest` | timestamp 非数字、超深嵌套、nonce 存储语义、并发下 nonce 只能通过一次 |
| `ApiSignIntegrationTest` | MockMvc 端到端：两种模式通过 / 篡改拦截 / 防重放 / 缺参 / 路径参数 / 多值参数 |

### 支持的入参形态

以下形态均经真实 HTTP 验证（`demo/form/*` 接口）：

| 入参形态 | 说明 |
|---------|------|
| GET + query 参数 | `?name=x&age=18`，参数全部参与签名 |
| POST + 表单 | `application/x-www-form-urlencoded` |
| 路径参数 `@PathVariable` | 从 URI 模板解析后并入签名集，**可防止路径上的资源 ID 被篡改** |
| 多值参数 | `tags=a&tags=b`，取首个值参与签名 |
| PUT + JSON body | 同 JSON body 模式 |
| 中文 / 特殊字符 | 按 UTF-8 编解码，与 Java 端一致 |
| 空值参数 | 空字符串不参与签名（与 sa-token 口径一致） |
| 嵌套 JSON | 自动展平为 `a.b` / `a[0]` |
