package com.wxl52d41.apisign.client.sample;

import com.wxl52d41.apisign.client.SignClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 验证《第三方接入指南》文档里的期望值与代码示例真实可用
 */
public class DocExampleTest {

    // 文档 3.5 节：md5 期望值
    @Test
    public void testDocMd5Expectation() {
        SignClient c = new SignClient("thirdB", "SwqFmsKxcbq23", "md5");
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("name", "zhang");
        p.put("age", "18");
        p.put("sex", "女");
        Assertions.assertEquals("6f5e844a53e74363c2f6b24f64c4f0ff", c.createSign(p),
                "文档 3.5 节 md5 期望值不正确");
    }

    // 文档 3.5 节：hmac-sha256 期望值
    @Test
    public void testDocHmacExpectation() {
        SignClient c = new SignClient("testApp", "SwqFmsKxcbq23", "hmac-sha256");
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("name", "zhang");
        p.put("age", "18");
        p.put("sex", "女");
        Assertions.assertEquals("f2cb11bddf000a4c71f8bd280cab65a84d99f1b4c490a6fbbbe49fffec7203cb",
                c.createSign(p), "文档 3.5 节 hmac-sha256 期望值不正确");
    }

    // 文档 4.1 节：展平规则表
    @Test
    public void testDocFlattenTable() {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("b", 1);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("a", a);
        body.put("tags", java.util.Arrays.asList("x", "y"));

        Map<String, String> flat = new LinkedHashMap<>();
        SignClient.flatten(body, "", flat);
        Assertions.assertEquals("1", flat.get("a.b"));
        Assertions.assertEquals("x", flat.get("tags[0]"));
        Assertions.assertEquals("y", flat.get("tags[1]"));
    }

    // 文档 5.1 节：最简用法可编译可运行
    @Test
    public void testDocBasicUsage() {
        SignClient signClient = new SignClient("thirdA", "thirdA-secret-key-please-change", "hmac-sha256");
        Map<String, String> params = new LinkedHashMap<>();
        params.put("name", "nzxy");
        params.put("age", "18");

        SignClient.SignTrio trio = signClient.sign(params);
        Assertions.assertEquals(32, trio.getNonce().length());
        Assertions.assertEquals(64, trio.getSign().length(), "hmac-sha256 应为 64 位十六进制");
    }

    // 文档 5.3 节：intoParams 含三件套
    @Test
    public void testDocIntoParams() {
        SignClient signClient = new SignClient("thirdA", "k", "md5");
        Map<String, String> params = new LinkedHashMap<>();
        params.put("name", "nzxy");

        SignClient.SignTrio trio = signClient.sign(params);
        Map<String, String> form = trio.intoParams(params);

        Assertions.assertTrue(form.containsKey(SignClient.TIMESTAMP));
        Assertions.assertTrue(form.containsKey(SignClient.NONCE));
        Assertions.assertTrue(form.containsKey(SignClient.SIGN));
        Assertions.assertEquals("nzxy", form.get("name"));
    }

    // 文档 7.1 节：每次重试三件套必须不同
    @Test
    public void testNonceAlwaysFresh() {
        SignClient signClient = new SignClient("thirdA", "k", "md5");
        Map<String, String> params = new LinkedHashMap<>();
        params.put("name", "nzxy");

        String n1 = signClient.sign(params).getNonce();
        String n2 = signClient.sign(params).getNonce();
        Assertions.assertNotEquals(n1, n2, "两次加签的 nonce 必须不同");
    }
}
