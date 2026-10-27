package com.wxl52d41.apisign.starter.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wxl52d41.apisign.core.SignErrorCode;
import com.wxl52d41.apisign.core.SignException;
import com.wxl52d41.apisign.core.SignTemplate;
import com.wxl52d41.apisign.core.SignUtil;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 签名参数解析器：从请求中提取参与验签的参数集合
 *
 * 三种模式：
 * - 参数模式（Content-Type 非 application/json）：参数取 query / form，三件套也在参数中
 * - JSON body 展平模式（Content-Type = application/json，默认）：业务参数取 body 并展平，三件套从 header 读取
 * - JSON body 原文模式（@ApiSign(bodyMode = RAW)）：不解析 body，直接对 body 原文签名，三件套从 header 读取
 *
 * 内部使用独立的 ObjectMapper 解析 JSON，不注入宿主的 ObjectMapper，
 * 避免宿主自定义序列化规则（如数字精度、NULL 策略）导致展平口径漂移。
 *
 * @author wxl52d41
 * @since 1.0.0
 */
public class SignParamResolver {

    /**
     * JSON 展平的最大嵌套深度，超过则拒绝（防止恶意构造超深 JSON 导致 StackOverflowError）
     */
    private static final int MAX_FLATTEN_DEPTH = 32;

    /**
     * 专用于解析 JSON 的 ObjectMapper，避免受宿主配置影响
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从请求中解析参与验签的完整参数集合（含 timestamp / nonce / sign）
     *
     * @param request       请求
     * @param includeParams 参与签名的参数名白名单，为空代表全部参数参与
     * @return 参数集合
     */
    public Map<String, String> resolve(HttpServletRequest request, String... includeParams) {
        Map<String, String> paramMap = getSignParam(request);

        // 路径参数（@PathVariable）不在 getParameterMap 中，需按 URI 模板单独解析后并入，
        // 否则路径上的资源 ID 可被篡改而不影响验签
        putPathVariables(request, paramMap);

        // JSON body 模式：三件套从 header 读取并入参数集
        if (RepeatableReadFilter.isJsonBody(request)) {
            putIfNotEmpty(paramMap, SignTemplate.timestamp, request.getHeader(SignTemplate.timestamp));
            putIfNotEmpty(paramMap, SignTemplate.nonce, request.getHeader(SignTemplate.nonce));
            putIfNotEmpty(paramMap, SignTemplate.sign, request.getHeader(SignTemplate.sign));
        }

        // 白名单过滤：只保留 includeParams 中的参数，三件套必须保留
        if (includeParams != null && includeParams.length > 0) {
            Set<String> keep = new HashSet<>(Arrays.asList(includeParams));
            keep.add(SignTemplate.timestamp);
            keep.add(SignTemplate.nonce);
            keep.add(SignTemplate.sign);
            paramMap.keySet().retainAll(keep);
        }

        return paramMap;
    }

    /**
     * 解析 URI 模板中的路径变量并并入参数集合
     *
     * @param request  请求
     * @param paramMap 参数集合
     */
    private void putPathVariables(HttpServletRequest request, Map<String, String> paramMap) {
        Object patternAttr = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (patternAttr == null) {
            return;
        }
        String pattern = patternAttr.toString();
        if (pattern.indexOf('{') < 0) {
            return;
        }

        String path = request.getRequestURI().substring(request.getContextPath().length());
        String[] patternSegments = pattern.split("/");
        String[] pathSegments = path.split("/");
        if (patternSegments.length != pathSegments.length) {
            return;
        }

        for (int i = 0; i < patternSegments.length; i++) {
            String seg = patternSegments[i];
            if (seg.startsWith("{") && seg.endsWith("}")) {
                String name = seg.substring(1, seg.length() - 1);
                // 去掉正则约束写法 {id:\d+}
                int colon = name.indexOf(':');
                if (colon > 0) {
                    name = name.substring(0, colon);
                }
                String value = pathSegments[i];
                if (!SignUtil.isEmpty(name) && !SignUtil.isEmpty(value)) {
                    paramMap.put(name, UriUtils.decode(value, StandardCharsets.UTF_8));
                }
            }
        }
    }

    /**
     * 获取业务参数集合
     *
     * @param request 请求
     * @return 参数集合
     */
    private Map<String, String> getSignParam(HttpServletRequest request) {
        // 非 json body：参数模式，取 query / form 参数
        if (!RepeatableReadFilter.isJsonBody(request)) {
            Map<String, String> map = new LinkedHashMap<>();
            Map<String, String[]> parameterMap = request.getParameterMap();
            for (String name : parameterMap.keySet()) {
                String[] values = parameterMap.get(name);
                if (values != null && values.length > 0) {
                    map.put(name, values[0]);
                }
            }
            return map;
        }

        // json body 模式：读取 body 并展平
        String body = readBody(request);
        try {
            Map<String, String> map = new LinkedHashMap<>();
            if (SignUtil.isNotEmpty(body)) {
                JsonNode root = objectMapper.readTree(body);
                // JSON body 模式要求顶层为对象，数组/标量无法作为参数集合参与验签
                if (root == null || !root.isObject()) {
                    throw new SignException("请求体必须为 JSON 对象", SignErrorCode.CODE_12206);
                }
                flattenJsonNode(root, "", map);
            }
            return map;
        } catch (IOException e) {
            SignException signException = new SignException("读取或解析请求体失败：" + e.getMessage(), SignErrorCode.CODE_12206);
            signException.initCause(e);
            throw signException;
        }
    }

    /**
     * 读取请求体字符串（依赖 RepeatableReadFilter 完成可重复读包装）
     *
     * 公开方法：body 原文签名模式（RAW）需要拿到与客户端发送字节完全一致的原文
     *
     * @param request 请求
     * @return body 原文字符串，无 body 时返回空字符串
     */
    public String readBody(HttpServletRequest request) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            try (java.io.InputStream is = request.getInputStream()) {
                while ((n = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, n);
                }
            }
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            SignException signException = new SignException("读取请求体失败：" + e.getMessage(), SignErrorCode.CODE_12206);
            signException.initCause(e);
            throw signException;
        }
    }

    /**
     * 将 JSON 树展平为点号键的 Map
     *
     * @param node   JSON 节点
     * @param prefix 键前缀
     * @param out    输出的参数集合
     */
    public static void flattenJsonNode(JsonNode node, String prefix, Map<String, String> out) {
        flattenJsonNode(node, prefix, out, 0);
    }

    /**
     * 将 JSON 树展平为点号键的 Map（带深度保护）
     *
     * @param node   JSON 节点
     * @param prefix 键前缀
     * @param out    输出的参数集合
     * @param depth  当前嵌套深度
     */
    private static void flattenJsonNode(JsonNode node, String prefix, Map<String, String> out, int depth) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return;
        }
        if (depth > MAX_FLATTEN_DEPTH) {
            throw new SignException("请求体嵌套层级过深，超出允许范围：" + MAX_FLATTEN_DEPTH, SignErrorCode.CODE_12206);
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                flattenJsonNode(entry.getValue(), key, out, depth + 1);
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                flattenJsonNode(node.get(i), prefix + "[" + i + "]", out, depth + 1);
            }
        } else {
            out.put(prefix, node.asText());
        }
    }

    /**
     * 将 Java 对象展平为点号键的 Map（客户端生成签名时使用，展平规则与 flattenJsonNode 一致）
     *
     * 支持 Map / List / 数组 / 基本类型；其他 POJO 请先转 Map，否则字段无法展开
     *
     * @param value  待展平的对象
     * @param prefix 键前缀
     * @param out    输出的参数集合
     */
    public static void flattenObject(Object value, String prefix, Map<String, String> out) {
        if (value == null) {
            return;
        }
        if (value instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                String key = prefix.isEmpty() ? String.valueOf(entry.getKey())
                        : prefix + "." + entry.getKey();
                flattenObject(entry.getValue(), key, out);
            }
        } else if (value instanceof List) {
            List<?> list = (List<?>) value;
            for (int i = 0; i < list.size(); i++) {
                flattenObject(list.get(i), prefix + "[" + i + "]", out);
            }
        } else if (value instanceof Object[]) {
            Object[] array = (Object[]) value;
            for (int i = 0; i < array.length; i++) {
                flattenObject(array[i], prefix + "[" + i + "]", out);
            }
        } else {
            out.put(prefix, String.valueOf(value));
        }
    }

    private static void putIfNotEmpty(Map<String, String> map, String key, String value) {
        if (value != null && !"".equals(value)) {
            map.put(key, value);
        }
    }
}
