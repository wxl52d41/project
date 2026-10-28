package com.wxl52d41.apikey.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Model: API Key
 *
 * 拷贝自 sa-token 的 ApiKeyModel，去掉 SaJsonType / SaTokenDao 依赖：
 * - NEVER_EXPIRE 常量本地化
 * - 序列化交给宿主（starter 内提供 JSON 转换）
 *
 * @author click33 / wxl52d41
 * @since 1.0.0
 */
public class ApiKeyModel {

    /**
     * 代表永不过期的时间戳
     */
    public static final long NEVER_EXPIRE = -1;

    /**
     * 名称
     */
    private String title;

    /**
     * 介绍
     */
    private String intro;

    /**
     * ApiKey 值
     */
    private String apiKey;

    /**
     * 账号 id
     */
    private Object loginId;

    /**
     * ApiKey 创建时间，13位时间戳
     */
    private long createTime;

    /**
     * ApiKey 到期时间，13位时间戳 (-1=永不过期)
     */
    private long expiresTime;

    /**
     * 是否有效 (true=生效, false=禁用)
     */
    private Boolean isValid = true;

    /**
     * 授权范围
     */
    private List<String> scopes = new ArrayList<>();

    /**
     * 扩展数据
     */
    private Map<String, Object> extraData;

    /**
     * 构造函数
     */
    public ApiKeyModel() {
        this.createTime = System.currentTimeMillis();
    }

    /**
     * 添加 Scope
     *
     * @param scope scope 列表
     * @return 对象自身
     */
    public ApiKeyModel addScope(String... scope) {
        if (this.scopes == null) {
            this.scopes = new ArrayList<>();
        }
        this.scopes.addAll(Arrays.asList(scope));
        return this;
    }

    /**
     * 添加 扩展数据
     *
     * @param key 键
     * @param value 值
     * @return 对象自身
     */
    public ApiKeyModel addExtra(String key, Object value) {
        if (this.extraData == null) {
            this.extraData = new LinkedHashMap<>();
        }
        this.extraData.put(key, value);
        return this;
    }

    /**
     * 查询扩展数据
     *
     * @param key 键
     * @return 值
     */
    public Object getExtra(String key) {
        if (this.extraData == null) {
            return null;
        }
        return this.extraData.get(key);
    }

    /**
     * 删除扩展数据
     *
     * @param key 键
     * @return 被删除的值
     */
    public Object removeExtra(String key) {
        if (this.extraData == null) {
            return null;
        }
        return this.extraData.remove(key);
    }

    /**
     * 数据自检，判断是否可以保存入库
     */
    public void checkByCanSaved() {
        if (isEmpty(this.apiKey)) {
            throw new ApiKeyException("ApiKey 值不可为空").setApiKey(apiKey).setCode(ApiKeyErrorCode.CODE_12304);
        }
        if (this.loginId == null) {
            throw new ApiKeyException("无效 ApiKey: " + apiKey).setApiKey(apiKey).setCode(ApiKeyErrorCode.CODE_12304);
        }
        if (this.createTime == 0) {
            throw new ApiKeyException("请指定 createTime 创建时间").setApiKey(apiKey).setCode(ApiKeyErrorCode.CODE_12304);
        }
        if (this.expiresTime == 0) {
            throw new ApiKeyException("请指定 expiresTime 过期时间").setApiKey(apiKey).setCode(ApiKeyErrorCode.CODE_12304);
        }
        if (this.isValid == null) {
            throw new ApiKeyException("请指定 isValid 是否生效").setApiKey(apiKey).setCode(ApiKeyErrorCode.CODE_12304);
        }
    }

    /**
     * 获取：此 ApiKey 的剩余有效期（秒）, -1=永不过期
     *
     * @return 剩余秒数
     */
    public long expiresIn() {
        if (expiresTime == NEVER_EXPIRE) {
            return NEVER_EXPIRE;
        }
        long s = (expiresTime - System.currentTimeMillis()) / 1000;
        return s < 1 ? -2 : s;
    }

    /**
     * 判断：此 ApiKey 是否已超时
     *
     * @return 是否超时
     */
    public boolean timeExpired() {
        if (expiresTime == NEVER_EXPIRE) {
            return false;
        }
        return System.currentTimeMillis() > expiresTime;
    }

    private static boolean isEmpty(Object str) {
        return str == null || "".equals(str);
    }

    // get and set

    public String getTitle() {
        return this.title;
    }

    public ApiKeyModel setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getIntro() {
        return this.intro;
    }

    public ApiKeyModel setIntro(String intro) {
        this.intro = intro;
        return this;
    }

    public String getApiKey() {
        return this.apiKey;
    }

    public ApiKeyModel setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public Object getLoginId() {
        return this.loginId;
    }

    public ApiKeyModel setLoginId(Object loginId) {
        this.loginId = loginId;
        return this;
    }

    public long getCreateTime() {
        return this.createTime;
    }

    public ApiKeyModel setCreateTime(long createTime) {
        this.createTime = createTime;
        return this;
    }

    public long getExpiresTime() {
        return this.expiresTime;
    }

    public ApiKeyModel setExpiresTime(long expiresTime) {
        this.expiresTime = expiresTime;
        return this;
    }

    public Boolean getIsValid() {
        return this.isValid;
    }

    public ApiKeyModel setIsValid(Boolean isValid) {
        this.isValid = isValid;
        return this;
    }

    public List<String> getScopes() {
        return this.scopes;
    }

    public ApiKeyModel setScopes(List<String> scopes) {
        this.scopes = scopes;
        return this;
    }

    public Map<String, Object> getExtraData() {
        return this.extraData;
    }

    public ApiKeyModel setExtraData(Map<String, Object> extraData) {
        this.extraData = extraData;
        return this;
    }
}
