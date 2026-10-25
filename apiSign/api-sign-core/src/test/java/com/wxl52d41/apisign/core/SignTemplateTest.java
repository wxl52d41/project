package com.wxl52d41.apisign.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 签名算法测试：算法一致性、多 appId 隔离、防重放、异常码
 */
public class SignTemplateTest {

    private static final String KEY = "SwqFmsKxcbq23";

    private SignTemplate newTemplate() {
        return new SignTemplate(new MemoryNonceStore());
    }

    private SignApp app() {
        return new SignApp("testApp", KEY, "md5");
    }

    private SignApp app(String appId, String algo) {
        return new SignApp(appId, KEY, algo);
    }

    // 与 sa-token 官方单测期望值一致，证明算法互通
    @Test
    public void testCreateSign() {
        SignTemplate template = newTemplate();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "zhang");
        map.put("age", 18);
        map.put("sex", "女");

        Assertions.assertEquals("6f5e844a53e74363c2f6b24f64c4f0ff", template.createSign(map, app()));
    }

    @Test
    public void testJoinParamsDictSort() {
        SignTemplate template = newTemplate();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "zhang");
        map.put("age", 18);
        map.put("sex", "女");

        Assertions.assertEquals("age=18&name=zhang&sex=女", template.joinParamsDictSort(map));
    }

    // hmac-md5 口径：密钥参与 HMAC，不拼接 &key=
    @Test
    public void testHmacMd5() {
        SignTemplate template = newTemplate();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "zhang");
        map.put("age", 18);
        map.put("sex", "女");

        Assertions.assertEquals("269c49e0541d24e9732895db2af7944d",
                template.createSign(map, app("a", "hmac-md5")));
    }

    // hmac-sha256 口径
    @Test
    public void testHmacSha256() {
        SignTemplate template = newTemplate();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "zhang");
        map.put("age", 18);
        map.put("sex", "女");

        Assertions.assertEquals("f2cb11bddf000a4c71f8bd280cab65a84d99f1b4c490a6fbbbe49fffec7203cb",
                template.createSign(map, app("a", "hmac-sha256")));
    }

    // 不同 appId 使用不同密钥，签名结果不同
    @Test
    public void testDifferentAppsDifferentSign() {
        SignTemplate template = newTemplate();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "zhang");

        String sign1 = template.createSign(map, new SignApp("appA", "keyA"));
        String sign2 = template.createSign(map, new SignApp("appB", "keyB"));
        Assertions.assertNotEquals(sign1, sign2);
    }

    // nonce 存储 key 含 appId，实现隔离
    @Test
    public void testNonceKeyIsolation() {
        SignTemplate template = newTemplate();
        String keyA = template.splicingNonceSaveKey("n1", app("appA", "md5"));
        String keyB = template.splicingNonceSaveKey("n1", app("appB", "md5"));
        Assertions.assertNotEquals(keyA, keyB);
        Assertions.assertTrue(keyA.contains("appA"));
    }

    // 同一 appId 下 nonce 不可复用
    @Test
    public void testNonceReplay() {
        SignTemplate template = newTemplate();
        template.checkNonce("dup", app());
        SignException e = Assertions.assertThrows(SignException.class,
                () -> template.checkNonce("dup", app()));
        Assertions.assertEquals(SignErrorCode.CODE_12202, e.getCode());
    }

    // 不同 appId 使用相同 nonce 值互不影响
    @Test
    public void testSameNonceDifferentApp() {
        SignTemplate template = newTemplate();
        template.checkNonce("shared", app("appA", "md5"));
        Assertions.assertDoesNotThrow(() -> template.checkNonce("shared", app("appB", "md5")));
    }

    // 并发下同一 nonce 只允许一次通过
    @Test
    public void testNonceConcurrent() throws Exception {
        SignTemplate template = newTemplate();
        int threads = 32;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger passed = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        template.checkNonce("concurrent", app());
                        passed.incrementAndGet();
                    } catch (SignException ignored) {
                        // 预期：并发下只有一个通过
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            Assertions.assertTrue(done.await(10, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
        Assertions.assertEquals(1, passed.get());
    }

    // timestamp 超窗
    @Test
    public void testTimestampDisparity() {
        SignTemplate template = newTemplate();
        SignApp app = new SignApp("a", KEY).setTimestampDisparity(60_000);

        Assertions.assertDoesNotThrow(() -> template.checkTimestamp(System.currentTimeMillis(), app));
        SignException e = Assertions.assertThrows(SignException.class,
                () -> template.checkTimestamp(System.currentTimeMillis() - 120_000, app));
        Assertions.assertEquals(SignErrorCode.CODE_12203, e.getCode());
    }

    // timestamp 非数字转签名异常，不抛 NumberFormatException
    @Test
    public void testBadTimestamp() {
        SignTemplate template = newTemplate();
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("timestamp", "abc");
        paramMap.put("nonce", "n1");
        paramMap.put("sign", "s1");

        SignException e = Assertions.assertThrows(SignException.class,
                () -> template.checkParamMap(paramMap, app()));
        Assertions.assertEquals(SignErrorCode.CODE_12204, e.getCode());
        Assertions.assertFalse(template.isValidParamMap(paramMap, app()));
    }

    // 完整链路通过
    @Test
    public void testCheckParamMapOk() {
        SignTemplate template = newTemplate();
        Map<String, Object> map = new HashMap<>();
        map.put("name", "zhang");
        Map<String, Object> signed = template.addSignParams(map, app());

        Map<String, String> paramMap = new HashMap<>();
        signed.forEach((k, v) -> paramMap.put(k, String.valueOf(v)));
        Assertions.assertDoesNotThrow(() -> template.checkParamMap(paramMap, app()));
    }

    // 篡改参数后验签失败
    @Test
    public void testTamperedParam() {
        SignTemplate template = newTemplate();
        Map<String, Object> map = new HashMap<>();
        map.put("name", "zhang");
        Map<String, Object> signed = template.addSignParams(map, app());
        signed.put("name", "hacker");

        Map<String, String> paramMap = new HashMap<>();
        signed.forEach((k, v) -> paramMap.put(k, String.valueOf(v)));
        SignException e = Assertions.assertThrows(SignException.class,
                () -> template.checkParamMap(paramMap, new SignApp("testApp", KEY)));
        Assertions.assertEquals(SignErrorCode.CODE_12202, e.getCode());
    }

    // 密钥为空
    @Test
    public void testEmptySecretKey() {
        SignTemplate template = newTemplate();
        SignException e = Assertions.assertThrows(SignException.class,
                () -> template.createSign(new HashMap<>(), new SignApp("a", "")));
        Assertions.assertEquals(SignErrorCode.CODE_12201, e.getCode());
    }
}
