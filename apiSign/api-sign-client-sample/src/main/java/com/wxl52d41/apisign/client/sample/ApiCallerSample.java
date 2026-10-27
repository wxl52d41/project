package com.wxl52d41.apisign.client.sample;

import com.wxl52d41.apisign.client.SignClient;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 第三方客户端调用示例（零框架依赖，只用 JDK）
 *
 * 演示两种调用形态：
 * 1. query / form 参数模式：三件套随参数一起提交
 * 2. JSON body 模式：三件套放请求头
 *
 * 实际项目里通常用 HttpClient / RestTemplate / OkHttp / Feign，
 * 加签逻辑完全相同，区别只是把 SignTrio 里的值塞到请求头或参数里。
 *
 * @author wxl52d41
 * @since 1.0.0
 */
public class ApiCallerSample {

    /**
     * 服务端地址
     */
    private final String baseUrl;

    /**
     * 加签器
     */
    private final SignClient signClient;

    public ApiCallerSample(String baseUrl, String appId, String secretKey, String digestAlgo) {
        this.baseUrl = baseUrl;
        this.signClient = new SignClient(appId, secretKey, digestAlgo);
    }

    // ==================================================================
    // 方式一：query / form 参数模式
    // ==================================================================

    /**
     * POST 表单调用
     *
     * @param path   接口路径，如 /demo/form-body
     * @param params 业务参数
     * @return 响应内容
     */
    public String postForm(String path, Map<String, String> params) throws Exception {
        // 1. 业务参数参与加签 -> 得到三件套
        SignClient.SignTrio trio = signClient.sign(params);

        // 2. 三件套并入参数，一起作为表单提交
        Map<String, String> form = trio.intoParams(params);

        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, String> e : form.entrySet()) {
            if (body.length() > 0) {
                body.append('&');
            }
            body.append(URLEncoder.encode(e.getKey(), "UTF-8"))
                    .append('=')
                    .append(URLEncoder.encode(e.getValue(), "UTF-8"));
        }

        HttpURLConnection conn = open(path, "POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        // appId 走请求头，不参与签名
        conn.setRequestProperty(SignClient.APP_ID_HEADER, signClient.getAppId());
        return write(conn, body.toString());
    }

    /**
     * GET query 调用
     *
     * @param path   接口路径，如 /demo/form/query
     * @param params 业务参数
     * @return 响应内容
     */
    public String getQuery(String path, Map<String, String> params) throws Exception {
        SignClient.SignTrio trio = signClient.sign(params);
        Map<String, String> query = trio.intoParams(params);

        StringBuilder qs = new StringBuilder();
        for (Map.Entry<String, String> e : query.entrySet()) {
            if (qs.length() > 0) {
                qs.append('&');
            }
            qs.append(URLEncoder.encode(e.getKey(), "UTF-8"))
                    .append('=')
                    .append(URLEncoder.encode(e.getValue(), "UTF-8"));
        }

        HttpURLConnection conn = open(path + "?" + qs, "GET");
        conn.setRequestProperty(SignClient.APP_ID_HEADER, signClient.getAppId());
        return read(conn);
    }

    /**
     * 路径参数调用：路径变量同样参与签名
     *
     * @param pathPrefix 路径前缀，如 /demo/form/path/
     * @param pathParam  路径参数名，如 goodsId
     * @param pathValue  路径参数值，如 G001
     * @param params     其余 query 参数
     * @return 响应内容
     */
    public String getWithPathVariable(String pathPrefix, String pathParam, String pathValue,
                                      Map<String, String> params) throws Exception {
        // 路径参数必须一并参与签名
        Map<String, String> signParams = new LinkedHashMap<>(params);
        signParams.put(pathParam, pathValue);

        SignClient.SignTrio trio = signClient.sign(signParams);

        // 路径参数已放进 URL，其余参数（含三件套）作为 query
        Map<String, String> query = new LinkedHashMap<>(params);
        query.put(SignClient.TIMESTAMP, trio.getTimestamp());
        query.put(SignClient.NONCE, trio.getNonce());
        query.put(SignClient.SIGN, trio.getSign());

        StringBuilder qs = new StringBuilder();
        for (Map.Entry<String, String> e : query.entrySet()) {
            if (qs.length() > 0) {
                qs.append('&');
            }
            qs.append(URLEncoder.encode(e.getKey(), "UTF-8"))
                    .append('=')
                    .append(URLEncoder.encode(e.getValue(), "UTF-8"));
        }

        String url = pathPrefix + URLEncoder.encode(pathValue, "UTF-8") + "?" + qs;
        HttpURLConnection conn = open(url, "GET");
        conn.setRequestProperty(SignClient.APP_ID_HEADER, signClient.getAppId());
        return read(conn);
    }

    // ==================================================================
    // 方式二：JSON body 模式
    // ==================================================================

    /**
     * POST JSON body 调用（默认：body 原文签名）
     *
     * 对标 Stripe / GitHub webhook：sign = digest(timestamp + "\n" + nonce + "\n" + body原文)，
     * 不展平字段，body 只需传实际发送的字符串本身
     *
     * @param path     接口路径，如 /demo/json-body
     * @param jsonBody JSON body 原文字符串（签名与发送使用同一份，不可重新序列化）
     * @return 响应内容
     */
    public String postJson(String path, String jsonBody) throws Exception {
        SignClient.SignTrio trio = signClient.signRawBody(jsonBody);
        return sendJson(path, "POST", jsonBody, trio);
    }

    /**
     * PUT JSON body 调用（默认：body 原文签名）
     */
    public String putJson(String path, String jsonBody) throws Exception {
        SignClient.SignTrio trio = signClient.signRawBody(jsonBody);
        return sendJson(path, "PUT", jsonBody, trio);
    }

    /**
     * POST JSON body 调用（展平签名模式，仅服务端接口标注 bodyMode = FLATTEN 时使用）
     *
     * @param path     接口路径，如 /demo/json-body-flatten
     * @param jsonBody JSON body 原文字符串（实际发送）
     * @param bodyMap  与 jsonBody 内容一致的 Map（用于展平加签）
     * @return 响应内容
     */
    public String postJsonFlatten(String path, String jsonBody, Map<String, ?> bodyMap) throws Exception {
        SignClient.SignTrio trio = signClient.signJsonBody(bodyMap);
        return sendJson(path, "POST", jsonBody, trio);
    }

    /**
     * 发送 JSON 请求的公共逻辑：三件套放请求头，body 原样提交
     */
    private String sendJson(String path, String method, String jsonBody, SignClient.SignTrio trio) throws Exception {
        HttpURLConnection conn = open(path, method);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        for (Map.Entry<String, String> e : trio.toHeaders(signClient.getAppId()).entrySet()) {
            conn.setRequestProperty(e.getKey(), e.getValue());
        }
        return write(conn, jsonBody);
    }

    // ==================================================================
    // 底层 HTTP
    // ==================================================================

    private HttpURLConnection open(String pathOrQuery, String method) throws Exception {
        URL url = new URL(baseUrl + pathOrQuery);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        conn.setDoInput(true);
        return conn;
    }

    private String write(HttpURLConnection conn, String body) throws Exception {
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return read(conn);
    }

    private String read(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (is != null) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            is.close();
        }
        return "HTTP " + code + " " + new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }
}
