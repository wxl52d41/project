package com.wxl52d41.apisign.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * createBodySign 算法测试
 *
 * 锚点值由 Python 独立计算（hashlib / hmac），证明三段式拼接口径无歧义：
 * fullStr = timestamp + "\n" + nonce + "\n" + body 原文
 */
public class CreateBodySignTest {

    private static final String KEY = "SwqFmsKxcbq23";
    private static final String TIMESTAMP = "1740000000000";
    private static final String NONCE = "abc";
    private static final String BODY = "{\"name\":\"nzxy\",\"age\":18}";

    private SignApp app(String algo) {
        return new SignApp("testApp", KEY, algo);
    }

    // Python: hashlib.md5((fullStr + "&key=KEY").encode()).hexdigest()
    @Test
    public void testMd5Anchor() {
        String sign = new SignTemplate(null).createBodySign(TIMESTAMP, NONCE, BODY, app("md5"));
        Assertions.assertEquals("d42edbffe81f2e4188a18f57fb25b32e", sign);
    }

    // Python: hmac.new(KEY, fullStr, sha256).hexdigest()
    @Test
    public void testHmacSha256Anchor() {
        String sign = new SignTemplate(null).createBodySign(TIMESTAMP, NONCE, BODY, app("hmac-sha256"));
        Assertions.assertEquals("e4a868c78a2c900877f94a1105526ffea2425dc19bd37c440d7f4700c6dfe527", sign);
    }

    // Python: hmac.new(KEY, fullStr, md5).hexdigest()
    @Test
    public void testHmacMd5Anchor() {
        String sign = new SignTemplate(null).createBodySign(TIMESTAMP, NONCE, BODY, app("hmac-md5"));
        Assertions.assertEquals("336e0cf676556bee9dfe8111bdc7652e", sign);
    }

    // Python: hashlib.sha256((fullStr + "&key=KEY").encode()).hexdigest()
    @Test
    public void testSha256Anchor() {
        String sign = new SignTemplate(null).createBodySign(TIMESTAMP, NONCE, BODY, app("sha256"));
        Assertions.assertEquals("956fbd377941bfe863305e67dfd769ab6c34cf4cd48cce0c27b4100b3728b043", sign);
    }

    // body 内含换行符：三段式拼接不受影响，两端可稳定复算
    @Test
    public void testBodyWithNewline() {
        String bodyWithNl = "{\"remark\":\"line1\\nline2\"}";
        SignTemplate template = new SignTemplate(null);
        String sign = template.createBodySign(TIMESTAMP, NONCE, bodyWithNl, app("md5"));
        // 修改 body 任一字节，签名必须变化
        String sign2 = template.createBodySign(TIMESTAMP, NONCE,
                "{\"remark\":\"line1\\nline3\"}", app("md5"));
        Assertions.assertNotEquals(sign, sign2);
    }

    // null body：等价于空串参与签名，不抛 NPE
    @Test
    public void testNullBody() {
        String signNull = new SignTemplate(null).createBodySign(TIMESTAMP, NONCE, null, app("md5"));
        String signEmpty = new SignTemplate(null).createBodySign(TIMESTAMP, NONCE, "", app("md5"));
        Assertions.assertEquals(signEmpty, signNull);
    }

    // 中文 body：UTF-8 口径两端一致
    @Test
    public void testChineseBody() {
        String body = "{\"name\":\"张三\",\"remark\":\"测试&特殊=字符\"}";
        String sign = new SignTemplate(null).createBodySign(TIMESTAMP, NONCE, body, app("hmac-sha256"));
        Assertions.assertEquals(64, sign.length());
    }

    // 空密钥必须拒绝（与其他 createSign 相同的 12201 行为）
    @Test
    public void testEmptyKey() {
        SignException e = Assertions.assertThrows(SignException.class,
                () -> new SignTemplate(null).createBodySign(TIMESTAMP, NONCE, BODY,
                        new SignApp("a", "")));
        Assertions.assertEquals(SignErrorCode.CODE_12201, e.getCode());
    }
}
