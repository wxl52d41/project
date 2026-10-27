package com.wxl52d41.apisign.client.sample;

import com.wxl52d41.apisign.client.SignClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * signRawBody 客户端加签测试：三件套生成与口径
 */
public class SignRawBodyTest {

    private static final String BODY = "{\"name\":\"nzxy\",\"age\":18}";

    // 基本生成：三件套齐全，签名长度符合算法
    @Test
    public void testSignTrio() {
        SignClient client = new SignClient("thirdA", "k", "hmac-sha256");
        SignClient.SignTrio trio = client.signRawBody(BODY);

        Assertions.assertNotNull(trio.getTimestamp());
        Assertions.assertEquals(32, trio.getNonce().length());
        Assertions.assertEquals(64, trio.getSign().length());
    }

    // 客户端签名可由服务端算法复算（同口径闭环）
    @Test
    public void testClientServerConsistency() {
        SignClient client = new SignClient("thirdA", "SwqFmsKxcbq23", "hmac-sha256");
        SignClient.SignTrio trio = client.signRawBody(BODY);

        String serverSide = new com.wxl52d41.apisign.core.SignTemplate(null)
                .createBodySign(trio.getTimestamp(), trio.getNonce(), BODY,
                        client.getApp());
        Assertions.assertEquals(serverSide, trio.getSign());
    }

    // 两次加签 nonce 必不同（重试必须重新生成）
    @Test
    public void testNonceFresh() {
        SignClient client = new SignClient("thirdA", "k", "md5");
        String n1 = client.signRawBody(BODY).getNonce();
        String n2 = client.signRawBody(BODY).getNonce();
        Assertions.assertNotEquals(n1, n2);
    }

    // body 改动一个字节签名即变（防篡改根基）
    @Test
    public void testTamperDetection() {
        SignClient client = new SignClient("thirdA", "k", "hmac-sha256");
        String s1 = client.signRawBody(BODY).getSign();
        String s2 = client.signRawBody("{\"name\":\"nzxy\",\"age\":19}").getSign();
        Assertions.assertNotEquals(s1, s2);
    }

    // 中文 body 正常加签
    @Test
    public void testChinese() {
        SignClient client = new SignClient("thirdA", "k", "md5");
        SignClient.SignTrio trio = client.signRawBody("{\"name\":\"张三\"}");
        Assertions.assertEquals(32, trio.getSign().length());
    }

    // toHeaders 输出完整四头（appId + 三件套）
    @Test
    public void testToHeaders() {
        SignClient client = new SignClient("thirdA", "k", "md5");
        SignClient.SignTrio trio = client.signRawBody(BODY);
        java.util.Map<String, String> h = trio.toHeaders("thirdA");

        Assertions.assertEquals("thirdA", h.get(SignClient.APP_ID_HEADER));
        Assertions.assertEquals(trio.getTimestamp(), h.get(SignClient.TIMESTAMP));
        Assertions.assertEquals(trio.getNonce(), h.get(SignClient.NONCE));
        Assertions.assertEquals(trio.getSign(), h.get(SignClient.SIGN));
    }
}
