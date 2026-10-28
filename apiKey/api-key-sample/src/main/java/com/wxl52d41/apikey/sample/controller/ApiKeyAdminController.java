package com.wxl52d41.apikey.sample.controller;

import com.wxl52d41.apikey.core.ApiKeyErrorCode;
import com.wxl52d41.apikey.core.ApiKeyException;
import com.wxl52d41.apikey.core.ApiKeyModel;
import com.wxl52d41.apikey.core.ApiKeyTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API Key 平台审批管理端：申请单 + 审批流
 *
 * 对应市场审批型开放平台（高德 / 微信开放平台）的申请-审批模式：
 * 第三方提交申请（创建即 isValid=false，审批中调用返回 12303）
 * -> 管理员审批通过（isValid=true，即刻可用）/ 驳回（直接删除申请）
 *
 * 状态机基于现有字段实现，core 零改动：
 * - 审批中：isValid=false + extraData.applyStatus=PENDING
 * - 已通过：isValid=true  + extraData.applyStatus=APPROVED + extraData.approvedBy
 * - 已驳回：模型删除（驳回原因存 extraData 供前端回显后由前端展示，不再落库）
 *
 * @author wxl52d41
 * @since 1.0.0
 */
@RestController
public class ApiKeyAdminController {

    /**
     * 申请单状态（存于 extraData.applyStatus）
     */
    private static final String KEY_APPLY_STATUS = "applyStatus";
    private static final String KEY_APPLY_REASON = "applyReason";
    private static final String KEY_APPROVED_BY = "approvedBy";

    private final ApiKeyTemplate apiKeyTemplate;

    public ApiKeyAdminController(ApiKeyTemplate apiKeyTemplate) {
        this.apiKeyTemplate = apiKeyTemplate;
    }

    /**
     * 第三方提交申请：创建一个处于「审批中」的 Key（isValid=false，调用返回 12303）
     *
     * @param loginId 绑定的用户 id
     * @param applyReason 申请理由（用途说明）
     * @param scopes 申请的权限范围（逗号分隔，可空）
     * @return 申请结果（含申请单 Key）
     */
    @RequestMapping("/applyApiKey")
    public Map<String, Object> applyApiKey(@RequestParam Object loginId,
                                           @RequestParam String applyReason,
                                           @RequestParam(required = false) String scopes) {
        ApiKeyModel ak = apiKeyTemplate.createApiKeyModel(loginId)
                .setTitle("申请审批中")
                .addExtra(KEY_APPLY_STATUS, "PENDING")
                .addExtra(KEY_APPLY_REASON, applyReason == null ? "" : applyReason);
        if (scopes != null && !scopes.trim().isEmpty()) {
            for (String s : scopes.split(",")) {
                if (!s.trim().isEmpty()) {
                    ak.addScope(s.trim());
                }
            }
        }
        // 创建即禁用：审批中的 Key 不可调用（12303）
        ak.setIsValid(false);
        apiKeyTemplate.saveApiKey(ak);
        return result(ak);
    }

    /**
     * 管理员查询待审批列表（该用户全部申请单，前端按 applyStatus 过滤展示）
     *
     * @param loginId 用户 id
     * @return 申请单列表
     */
    @RequestMapping("/admin/pendingList")
    public Map<String, Object> pendingList(@RequestParam Object loginId) {
        List<ApiKeyModel> all = apiKeyTemplate.getApiKeyList(loginId);
        List<Map<String, Object>> pending = new ArrayList<>();
        List<Map<String, Object>> approved = new ArrayList<>();
        for (ApiKeyModel ak : all) {
            Map<String, Object> item = new HashMap<>();
            item.put("apiKey", ak.getApiKey());
            item.put("title", ak.getTitle());
            item.put("applyReason", ak.getExtra(KEY_APPLY_REASON));
            item.put("applyStatus", ak.getExtra(KEY_APPLY_STATUS));
            item.put("approvedBy", ak.getExtra(KEY_APPROVED_BY));
            item.put("isValid", ak.getIsValid());
            item.put("scopes", ak.getScopes());
            item.put("expiresTime", ak.getExpiresTime());
            if ("PENDING".equals(ak.getExtra(KEY_APPLY_STATUS))) {
                pending.add(item);
            } else {
                approved.add(item);
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("pending", pending);
        data.put("history", approved);
        return result(data);
    }

    /**
     * 管理员审批通过：isValid=true，Key 即刻可用
     *
     * @param apiKey 申请单的 Key
     * @param approvedBy 审批人（记录审计）
     * @param title 审批后重命名（用途名，可空则保留申请名）
     * @return 审批结果
     */
    @RequestMapping("/admin/approve")
    public Map<String, Object> approve(@RequestParam String apiKey,
                                       @RequestParam String approvedBy,
                                       @RequestParam(required = false) String title) {
        ApiKeyModel ak = mustGet(apiKey);
        ak.setIsValid(true);
        ak.addExtra(KEY_APPLY_STATUS, "APPROVED");
        ak.addExtra(KEY_APPROVED_BY, approvedBy);
        if (title != null && !title.trim().isEmpty()) {
            ak.setTitle(title.trim());
        }
        apiKeyTemplate.saveApiKey(ak);
        return result("已通过，Key 即刻可用");
    }

    /**
     * 管理员驳回：删除申请单（该 Key 永远不可用）
     *
     * @param apiKey 申请单的 Key
     * @param approvedBy 审批人
     * @return 驳回结果
     */
    @RequestMapping("/admin/reject")
    public Map<String, Object> reject(@RequestParam String apiKey,
                                      @RequestParam String approvedBy) {
        ApiKeyModel ak = mustGet(apiKey);
        apiKeyTemplate.deleteApiKey(apiKey);
        return result("已驳回并删除，Key 作废（审批人：" + approvedBy + "）");
    }

    /**
     * 按审批人视角：驳回时返回带原因的提示（前端展示用）
     */
    private ApiKeyModel mustGet(String apiKey) {
        ApiKeyModel ak = apiKeyTemplate.getApiKey(apiKey);
        if (ak == null) {
            throw new ApiKeyException("申请单不存在: " + apiKey)
                    .setApiKey(apiKey).setCode(ApiKeyErrorCode.CODE_12301);
        }
        return ak;
    }

    private Map<String, Object> result(Object data) {
        Map<String, Object> m = new HashMap<>();
        m.put("code", 200);
        m.put("data", data);
        return m;
    }
}
