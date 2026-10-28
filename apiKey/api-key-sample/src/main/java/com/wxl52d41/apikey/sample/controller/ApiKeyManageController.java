package com.wxl52d41.apikey.sample.controller;

import com.wxl52d41.apikey.core.ApiKeyModel;
import com.wxl52d41.apikey.core.ApiKeyTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API Key 管理接口：创建 / 查询 / 修改 / 删除
 *
 * 对应 sa-token demo 的 ApiKeyController。
 * 真实项目中 loginId 应取自登录态（如 StpUtil.getLoginId()），本示例用参数传入。
 * 注意：loginId 必须 @RequestParam 显式声明——裸 Object 参数会被 Spring 按
 * @ModelAttribute 绑定成空实例，导致序列化失败
 *
 * @author wxl52d41
 * @since 1.0.0
 */
@RestController
public class ApiKeyManageController {

    private final ApiKeyTemplate apiKeyTemplate;

    public ApiKeyManageController(ApiKeyTemplate apiKeyTemplate) {
        this.apiKeyTemplate = apiKeyTemplate;
    }

    /**
     * 返回指定用户拥有的 ApiKey 列表
     *
     * @param loginId 用户 id
     * @return 列表
     */
    @RequestMapping("/myApiKeyList")
    public Map<String, Object> myApiKeyList(@RequestParam Object loginId) {
        List<ApiKeyModel> apiKeyList = apiKeyTemplate.getApiKeyList(loginId);
        return result(apiKeyList);
    }

    /**
     * 为指定用户创建一个新的 ApiKey
     *
     * @param loginId 用户 id
     * @return 新建的 Key
     */
    @RequestMapping("/createApiKey")
    public Map<String, Object> createApiKey(@RequestParam Object loginId) {
        ApiKeyModel akModel = apiKeyTemplate.createApiKeyModel(loginId).setTitle("test");
        apiKeyTemplate.saveApiKey(akModel);
        return result(akModel);
    }

    /**
     * 修改 ApiKey（先校验归属再保存）
     *
     * @param akModel 前端提交的模型
     * @return 结果
     */
    @RequestMapping("/updateApiKey")
    public Map<String, Object> updateApiKey(ApiKeyModel akModel) {
        // 先验证一下是否为本人的 ApiKey
        apiKeyTemplate.checkApiKeyLoginId(akModel.getApiKey(), akModel.getLoginId());
        // 修改
        ApiKeyModel akModel2 = apiKeyTemplate.getApiKey(akModel.getApiKey());
        akModel2.setTitle(akModel.getTitle());
        akModel2.setExpiresTime(akModel.getExpiresTime());
        akModel2.setIsValid(akModel.getIsValid());
        akModel2.setScopes(akModel.getScopes());
        apiKeyTemplate.saveApiKey(akModel2);
        return result("ok");
    }

    /**
     * 删除指定 ApiKey
     *
     * @param apiKey apiKey 值
     * @param loginId 用户 id（校验归属）
     * @return 结果
     */
    @RequestMapping("/deleteApiKey")
    public Map<String, Object> deleteApiKey(String apiKey, @RequestParam Object loginId) {
        apiKeyTemplate.checkApiKeyLoginId(apiKey, loginId);
        apiKeyTemplate.deleteApiKey(apiKey);
        return result("ok");
    }

    /**
     * 删除指定用户所有 ApiKey
     *
     * @param loginId 用户 id
     * @return 结果
     */
    @RequestMapping("/deleteMyAllApiKey")
    public Map<String, Object> deleteMyAllApiKey(@RequestParam Object loginId) {
        apiKeyTemplate.deleteApiKeyByLoginId(loginId);
        return result("ok");
    }

    /**
     * 统一响应结构（Java 8 兼容，不用 Map.of）
     */
    private Map<String, Object> result(Object data) {
        Map<String, Object> m = new HashMap<>();
        m.put("code", 200);
        m.put("data", data);
        return m;
    }
}
