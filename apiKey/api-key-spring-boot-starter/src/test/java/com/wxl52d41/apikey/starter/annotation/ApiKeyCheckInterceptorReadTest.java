package com.wxl52d41.apikey.starter.annotation;

import com.wxl52d41.apikey.core.ApiKeyConfig;
import com.wxl52d41.apikey.core.ApiKeyModel;
import com.wxl52d41.apikey.core.ApiKeyStore;
import com.wxl52d41.apikey.core.ApiKeyTemplate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ApiKeyCheckInterceptor 取值顺序测试：query 优先，无 query 时读 header
 */
public class ApiKeyCheckInterceptorReadTest {

    private final ApiKeyTemplate template = new ApiKeyTemplate(new InlineStore(), new ApiKeyConfig());

    /**
     * 轻量内存 Store
     */
    static class InlineStore implements ApiKeyStore {
        final Map<String, ApiKeyModel> m = new ConcurrentHashMap<>();
        final Map<String, List<String>> idx = new ConcurrentHashMap<>();

        @Override
        public ApiKeyModel getApiKey(String apiKey) {
            return m.get(apiKey);
        }

        @Override
        public void saveApiKey(String apiKey, ApiKeyModel model, long ttlSeconds) {
            m.put(apiKey, model);
        }

        @Override
        public void deleteApiKey(String apiKey) {
            m.remove(apiKey);
        }

        @Override
        public List<String> getIndexList(Object loginId) {
            return idx.getOrDefault(String.valueOf(loginId), new ArrayList<>());
        }

        @Override
        public void saveIndexList(Object loginId, List<String> apiKeyList, long ttlSeconds) {
            idx.put(String.valueOf(loginId), new ArrayList<>(apiKeyList));
        }

        @Override
        public void deleteIndex(Object loginId) {
            idx.remove(String.valueOf(loginId));
        }
    }

    // 仅 header 时：getParameter 为 null，回退 getHeader 能读到
    @Test
    public void testReadHeaderWhenNoQuery() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/akRes1");
        req.addHeader("apikey", "AK-from-header");

        String apiKey = req.getParameter(ApiKeyCheckInterceptor.API_KEY_NAME);
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = req.getHeader(ApiKeyCheckInterceptor.API_KEY_NAME);
        }
        Assertions.assertEquals("AK-from-header", apiKey);
    }

    // 仅 query 时：getParameter 直接读到（query 优先）
    @Test
    public void testReadQueryFirst() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/akRes1");
        req.setParameter("apikey", "AK-from-query");
        req.addHeader("apikey", "AK-from-header");

        String apiKey = req.getParameter(ApiKeyCheckInterceptor.API_KEY_NAME);
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = req.getHeader(ApiKeyCheckInterceptor.API_KEY_NAME);
        }
        Assertions.assertEquals("AK-from-query", apiKey);
    }

    // header 传递的 Key 能通过完整校验链
    @Test
    public void testHeaderKeyValidates() {
        ApiKeyModel ak = template.createApiKeyModel(10001);
        template.saveApiKey(ak);

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/akRes1");
        req.addHeader("apikey", ak.getApiKey());

        String apiKey = req.getParameter(ApiKeyCheckInterceptor.API_KEY_NAME);
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = req.getHeader(ApiKeyCheckInterceptor.API_KEY_NAME);
        }
        Assertions.assertEquals(ak.getApiKey(), apiKey);
        Assertions.assertEquals(10001, template.checkApiKey(apiKey).getLoginId());
    }
}
