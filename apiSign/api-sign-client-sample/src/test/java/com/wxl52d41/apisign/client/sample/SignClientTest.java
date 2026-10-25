package com.wxl52d41.apisign.client.sample;

import com.wxl52d41.apisign.client.SignClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 客户端加签口径测试：确保与服务端算法一致
 */
public class SignClientTest {

    // 与服务端 SignTemplateTest 相同的锚点，证明两端算法互通
    @Test
    public void testSignMatchesServerAnchor() {
        SignClient client = new SignClient("testApp", "SwqFmsKxcbq23", "md5");

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", "zhang");
        params.put("age", 18);
        params.put("sex", "女");

        Assertions.assertEquals("6f5e844a53e74363c2f6b24f64c4f0ff", client.createSign(params));
    }

    // HMAC-SHA256 口径
    @Test
    public void testHmacSha256() {
        SignClient client = new SignClient("testApp", "SwqFmsKxcbq23", "hmac-sha256");

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", "zhang");
        params.put("age", 18);
        params.put("sex", "女");

        Assertions.assertEquals("f2cb11bddf000a4c71f8bd280cab65a84d99f1b4c490a6fbbbe49fffec7203cb",
                client.createSign(params));
    }

    // 三件套必须同时返回，且 timestamp/nonce 参与签名
    @Test
    public void testSignTrio() {
        SignClient client = new SignClient("testApp", "SwqFmsKxcbq23", "md5");

        Map<String, String> params = new LinkedHashMap<>();
        params.put("name", "nzxy");

        SignClient.SignTrio trio = client.sign(params);
        Assertions.assertNotNull(trio.getTimestamp());
        Assertions.assertNotNull(trio.getNonce());
        Assertions.assertNotNull(trio.getSign());

        // 用返回的参数重新验算，结果应一致
        Map<String, String> verify = trio.intoParams(params);
        Map<String, Object> verifyMap = new LinkedHashMap<>(verify);
        verifyMap.remove(SignClient.SIGN);
        Assertions.assertEquals(trio.getSign(), client.createSign(verifyMap));
    }

    // JSON 展平：嵌套对象、数组、null
    @Test
    public void testFlatten() {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("name", "nzxy");
        user.put("age", 18);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("user", user);
        body.put("tags", java.util.Arrays.asList("a", "b"));
        body.put("nil", null);

        Map<String, String> flat = new LinkedHashMap<>();
        SignClient.flatten(body, "", flat);

        Map<String, String> expect = new LinkedHashMap<>();
        expect.put("user.name", "nzxy");
        expect.put("user.age", "18");
        expect.put("tags[0]", "a");
        expect.put("tags[1]", "b");
        Assertions.assertEquals(expect, flat);
    }

    // appId 请求头
    @Test
    public void testHeaders() {
        SignClient client = new SignClient("thirdA", "key", "md5");
        SignClient.SignTrio trio = client.sign(new LinkedHashMap<String, String>());

        Map<String, String> headers = trio.toHeaders(client.getAppId());
        Assertions.assertEquals("thirdA", headers.get(SignClient.APP_ID_HEADER));
        Assertions.assertEquals(trio.getSign(), headers.get(SignClient.SIGN));
    }
}
