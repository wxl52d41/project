package com.wxl52d41.apisign.client;

import com.wxl52d41.apisign.core.SignApp;
import com.wxl52d41.apisign.core.SignTemplate;
import com.wxl52d41.apisign.core.SignUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 第三方客户端加签器
 *
 * 服务端验签时要求的三件套（timestamp / nonce / sign）由本类生成，
 * 并封装了 JSON body 展平规则，保证与服务端口径完全一致。
 *
 * 使用方式见 README：第三方只需依赖 api-sign-core，无需 Spring。
 *
 * @author wxl52d41
 * @since 1.0.0
 */
public class SignClient {

    /**
     * 服务端约定的接入方标识请求头名称
     */
    public static final String APP_ID_HEADER = "X-Sign-App-Id";

    /**
     * 三件套参数名（与服务端 SignTemplate 常量保持一致）
     */
    public static final String TIMESTAMP = SignTemplate.timestamp;
    public static final String NONCE = SignTemplate.nonce;
    public static final String SIGN = SignTemplate.sign;

    private final SignApp app;

    /**
     * 无状态用法：客户端只做加签，不需要 nonce 存储
     */
    public SignClient(String appId, String secretKey, String digestAlgo) {
        this.app = new SignApp(appId, secretKey, digestAlgo);
    }

    public SignClient(SignApp app) {
        this.app = app;
    }

    public String getAppId() {
        return app.getAppId();
    }

    public SignApp getApp() {
        return app;
    }

    /**
     * 生成 timestamp / nonce / sign 三件套
     *
     * 注意：timestamp 与 nonce 参与签名，必须先补齐再计算 sign
     *
     * @param params 业务参数（扁平 Map）
     * @return 三件套
     */
    public SignTrio sign(Map<String, ?> params) {
        Map<String, Object> signMap = new LinkedHashMap<>(params);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = SignUtil.getRandomString(32);
        signMap.put(TIMESTAMP, timestamp);
        signMap.put(NONCE, nonce);

        String sign = createSign(signMap);
        return new SignTrio(timestamp, nonce, sign);
    }

    /**
     * 对 JSON body 加签：自动展平嵌套结构后生成三件套
     *
     * @param body JSON body（Map 或可转换为 Map 的对象，建议直接传 Map）
     * @return 三件套
     */
    public SignTrio signJsonBody(Map<String, ?> body) {
        Map<String, String> flat = new LinkedHashMap<>();
        flatten(body, "", flat);
        return sign(flat);
    }

    /**
     * 对 JSON body 原文加签：不展平、不解析，直接对 timestamp + "\n" + nonce + "\n" + body 原文签名
     *
     * 对标 Stripe / GitHub webhook 模式，任意语言零门槛。
     * 关键：rawBody 必须是实际发送的 body 字符串本身，不能用 JSON 库重新序列化
     * （重排 key、改空白都会导致验签失败）
     *
     * @param rawBody body 原文字符串
     * @return 三件套
     */
    public SignTrio signRawBody(String rawBody) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = SignUtil.getRandomString(32);

        // 客户端加签不需要 nonce 存储，传 null 即可（SignTemplate 仅在验签时访问）
        String sign = new SignTemplate(null).createBodySign(timestamp, nonce, rawBody, app);
        return new SignTrio(timestamp, nonce, sign);
    }

    /**
     * 直接计算签名（传入的参数需已包含 timestamp 与 nonce）
     *
     * @param params 已含三件套中 timestamp / nonce 的参数
     * @return 签名
     */
    public String createSign(Map<String, ?> params) {
        // 客户端加签不需要 nonce 存储，传 null 即可（SignTemplate 仅在验签时访问）
        return new SignTemplate(null).createSign(params, app);
    }

    /**
     * 与服务端完全一致的展平规则：嵌套对象点号连接，数组用下标，null 跳过
     *
     * @param value  待展平对象
     * @param prefix 键前缀
     * @param out    输出
     */
    public static void flatten(Object value, String prefix, Map<String, String> out) {
        if (value == null) {
            return;
        }
        if (value instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                String key = prefix.isEmpty() ? String.valueOf(entry.getKey())
                        : prefix + "." + entry.getKey();
                flatten(entry.getValue(), key, out);
            }
        } else if (value instanceof Iterable) {
            int i = 0;
            for (Object item : (Iterable<?>) value) {
                flatten(item, prefix + "[" + i + "]", out);
                i++;
            }
        } else if (value instanceof Object[]) {
            Object[] array = (Object[]) value;
            for (int i = 0; i < array.length; i++) {
                flatten(array[i], prefix + "[" + i + "]", out);
            }
        } else {
            out.put(prefix, String.valueOf(value));
        }
    }

    /**
     * 三件套
     */
    public static class SignTrio {

        private final String timestamp;

        private final String nonce;

        private final String sign;

        public SignTrio(String timestamp, String nonce, String sign) {
            this.timestamp = timestamp;
            this.nonce = nonce;
            this.sign = sign;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getNonce() {
            return nonce;
        }

        public String getSign() {
            return sign;
        }

        /**
         * 把三件套并入参数集合（用于 query / form 参数模式）
         *
         * @param params 业务参数
         * @return 含三件套的新 Map
         */
        public Map<String, String> intoParams(Map<String, String> params) {
            Map<String, String> m = new LinkedHashMap<>(params);
            m.put(TIMESTAMP, timestamp);
            m.put(NONCE, nonce);
            m.put(SIGN, sign);
            return m;
        }

        /**
         * 转为请求头（用于 JSON body 模式，appId 也一并带上）
         *
         * @param appId 接入方标识
         * @return 请求头 Map
         */
        public Map<String, String> toHeaders(String appId) {
            Map<String, String> h = new LinkedHashMap<>();
            h.put(APP_ID_HEADER, appId);
            h.put(TIMESTAMP, timestamp);
            h.put(NONCE, nonce);
            h.put(SIGN, sign);
            return h;
        }

        @Override
        public String toString() {
            return "timestamp=" + timestamp + "&nonce=" + nonce + "&sign=" + sign;
        }
    }
}
