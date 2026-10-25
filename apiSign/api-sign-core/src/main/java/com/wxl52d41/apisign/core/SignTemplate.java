package com.wxl52d41.apisign.core;

import java.util.Map;
import java.util.TreeMap;

/**
 * API 参数签名算法模板，在跨系统接口调用时防参数篡改、防重放攻击。
 *
 * 核心算法拷贝自 sa-token 的 SaSignTemplate，签名结果与 sa-token-sign 保持一致，
 * 双方（本模块服务端 / sa-token 客户端）可以互相验签。
 *
 * 本类不持有接入方密钥，密钥与算法参数由调用方按 appId 传入（见 SignApp），
 * 因此可作为无状态单例使用，天然支持多接入方。
 *
 * 流程大致如下：
 * 1. 以 md5( loginId={账号id}&nonce={随机字符串}&timestamp={13位时间戳}&key={secretkey秘钥} ) 生成签名 sign。
 * 2. 将 sign 随请求提交（参数模式放 query/form，JSON body 模式放 header）。
 * 3. 服务端接收到请求后，以同样的算法生成一次 sign 。
 * 4. 对比两次 sign 是否一致，一致则通过，否则拒绝 。
 *
 * @author click33 / wxl52d41
 * @since 1.0.0
 */
public class SignTemplate {

    /**
     * nonce 防重放存储
     */
    private final NonceStore nonceStore;

    public SignTemplate(NonceStore nonceStore) {
        this.nonceStore = nonceStore;
    }

    // ----------- 自定义使用的参数名称 (不声明final，允许开发者自定义修改)

    public static String key = "key";
    public static String timestamp = "timestamp";
    public static String nonce = "nonce";
    public static String sign = "sign";

    // ----------- 拼接参数

    /**
     * 将所有参数连接成一个字符串(不排序)，形如：b=2&a=1&c=3
     *
     * @param paramsMap 参数列表
     * @return 拼接出的参数字符串
     */
    public String joinParams(Map<String, ?> paramsMap) {

        // 按照 k1=v1&k2=v2&k3=v3 排列
        StringBuilder sb = new StringBuilder();
        for (String k : paramsMap.keySet()) {
            Object value = paramsMap.get(k);
            if (!SignUtil.isEmpty(value)) {
                sb.append(k).append("=").append(value).append("&");
            }
        }

        // 删除最后一位 &
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
    }

    /**
     * 将所有参数按照字典顺序连接成一个字符串，形如：a=1&b=2&c=3
     *
     * @param paramsMap 参数列表
     * @return 拼接出的参数字符串
     */
    public String joinParamsDictSort(Map<String, ?> paramsMap) {
        // 保证字段按照字典顺序排列
        if (!(paramsMap instanceof TreeMap)) {
            paramsMap = new TreeMap<>(paramsMap);
        }

        // 拼接
        return joinParams(paramsMap);
    }

    // ----------- 创建签名

    /**
     * 创建签名：md5(paramsStr + keyStr) 或 hmac(key, paramsStr)
     *
     * @param paramsMap 参数列表
     * @param app       接入方配置（提供密钥与算法）
     * @return 签名
     */
    public String createSign(Map<String, ?> paramsMap, SignApp app) {
        String secretKey = app.getSecretKey();
        SignException.notEmpty(secretKey, "参与参数签名的秘钥不可为空", SignErrorCode.CODE_12201);

        // 如果调用者不小心传入了 sign 参数，则此处需要将 sign 参数排除在外
        if (paramsMap.containsKey(sign)) {
            // 为了保证不影响原有的 paramsMap，此处需要再复制一份
            paramsMap = new TreeMap<>(paramsMap);
            paramsMap.remove(sign);
        }

        // 计算签名
        String paramsStr = joinParamsDictSort(paramsMap);
        // HMAC 模式：密钥参与 HMAC 运算，不需要拼接在 fullStr 中
        // 非 HMAC 模式（md5/sha 等）：密钥拼接在 fullStr 末尾参与摘要
        String fullStr;
        if (isHmac(app)) {
            fullStr = paramsStr;
        } else {
            fullStr = paramsStr + "&" + key + "=" + secretKey;
        }
        return digestFullStr(fullStr, app);
    }

    /**
     * 使用摘要算法创建签名
     *
     * @param fullStr 待摘要的字符串
     * @param app     接入方配置
     * @return 签名
     */
    public String digestFullStr(String fullStr, SignApp app) {
        return dispatchDigest(app.getDigestAlgo(), app.getSecretKey(), fullStr);
    }

    /**
     * 根据摘要算法名称分发计算
     *
     * @param digestAlgo 摘要算法
     * @param secretKey  秘钥（HMAC 系列使用）
     * @param fullStr    待摘要的字符串
     * @return 签名
     */
    static String dispatchDigest(String digestAlgo, String secretKey, String fullStr) {
        if (digestAlgo == null) {
            digestAlgo = "md5";
        }
        switch (digestAlgo.toLowerCase()) {
            case "md5":
                return DigestUtils.md5(fullStr);
            case "sha1":
                return DigestUtils.sha1(fullStr);
            case "sha256":
                return DigestUtils.sha256(fullStr);
            case "sha384":
                return DigestUtils.sha384(fullStr);
            case "sha512":
                return DigestUtils.sha512(fullStr);
            case "hmac-md5":
                return DigestUtils.hmacMd5(secretKey, fullStr);
            case "hmac-sha256":
                return DigestUtils.hmacSha256(secretKey, fullStr);
            case "hmac-sha512":
                return DigestUtils.hmacSha512(secretKey, fullStr);
            default:
                throw new SignException("不支持的摘要算法：" + digestAlgo, SignErrorCode.CODE_12205);
        }
    }

    /**
     * 判断摘要算法是否为 HMAC 系列（hmac-md5、hmac-sha256、hmac-sha512）
     *
     * @param app 接入方配置
     * @return 是否 HMAC 算法
     */
    public boolean isHmac(SignApp app) {
        String digestAlgo = app.getDigestAlgo();
        return digestAlgo != null && digestAlgo.toLowerCase().startsWith("hmac-");
    }

    /**
     * 给 paramsMap 追加 timestamp、nonce、sign 三个参数（客户端生成签名时使用）
     *
     * @param paramsMap 参数列表
     * @param app       接入方配置
     * @return 加工后的参数列表
     */
    public Map<String, Object> addSignParams(Map<String, Object> paramsMap, SignApp app) {
        paramsMap.put(timestamp, String.valueOf(System.currentTimeMillis()));
        paramsMap.put(nonce, SignUtil.getRandomString(32));
        paramsMap.put(sign, createSign(paramsMap, app));
        return paramsMap;
    }

    // ----------- 校验签名

    /**
     * 判断：指定时间戳与当前时间戳的差距是否在允许的范围内
     *
     * @param timestamp 待校验的时间戳
     * @param app       接入方配置
     * @return 是否在允许的范围内
     */
    public boolean isValidTimestamp(long timestamp, SignApp app) {
        long allowDisparity = app.getTimestampDisparity();
        long disparity = Math.abs(System.currentTimeMillis() - timestamp);
        return allowDisparity == -1 || disparity <= allowDisparity;
    }

    /**
     * 校验：指定时间戳与当前时间戳的差距是否在允许的范围内，如果超出则抛出异常
     *
     * @param timestamp 待校验的时间戳
     * @param app       接入方配置
     */
    public void checkTimestamp(long timestamp, SignApp app) {
        if (!isValidTimestamp(timestamp, app)) {
            throw new SignException("timestamp 超出允许的范围：" + timestamp, SignErrorCode.CODE_12203);
        }
    }

    /**
     * 判断：随机字符串 nonce 是否有效。
     * 注意：同一 nonce 可以被多次判断有效，不会被缓存
     *
     * @param nonce 待判断的随机字符串
     * @param app   接入方配置
     * @return 是否有效
     */
    public boolean isValidNonce(String nonce, SignApp app) {
        // 为空代表无效
        if (SignUtil.isEmpty(nonce)) {
            return false;
        }

        // 校验此 nonce 是否已被使用过
        String nonceKey = splicingNonceSaveKey(nonce, app);
        return nonceStore.get(nonceKey) == null;
    }

    /**
     * 校验：随机字符串 nonce 是否有效，如果无效则抛出异常。
     * 注意：同一 nonce 只可以被校验通过一次，校验后将保存在缓存中，再次校验将无法通过
     *
     * @param nonce 待校验的随机字符串
     * @param app   接入方配置
     */
    public void checkNonce(String nonce, SignApp app) {
        // 为空代表无效
        if (SignUtil.isEmpty(nonce)) {
            throw new SignException("nonce 为空，无效", SignErrorCode.CODE_12204);
        }

        // 原子写入：并发场景下同一 nonce 只有一个请求能写入成功，其余均视为重放
        String nonceKey = splicingNonceSaveKey(nonce, app);
        boolean success = nonceStore.setIfAbsent(nonceKey, nonce, app.getSaveNonceExpire() * 2 + 2);
        if (!success) {
            throw new SignException("此 nonce 已被使用过，不可重复使用：" + nonce, SignErrorCode.CODE_12202);
        }
    }

    /**
     * 判断：给定的参数 生成的签名是否为有效签名
     *
     * @param paramsMap 参数列表
     * @param sign      待验证的签名
     * @param app       接入方配置
     * @return 签名是否有效
     */
    public boolean isValidSign(Map<String, ?> paramsMap, String sign, SignApp app) {
        String theSign = createSign(paramsMap, app);
        return theSign.equals(sign);
    }

    /**
     * 校验：给定的参数 生成的签名是否为有效签名，如果签名无效则抛出异常
     *
     * @param paramsMap 参数列表
     * @param sign      待验证的签名
     * @param app       接入方配置
     */
    public void checkSign(Map<String, ?> paramsMap, String sign, SignApp app) {
        if (!isValidSign(paramsMap, sign, app)) {
            throw new SignException("无效签名：" + sign, SignErrorCode.CODE_12202);
        }
    }

    /**
     * 解析 timestamp 字符串为 long，非数字时抛签名异常（避免 NumberFormatException 变成 500）
     *
     * @param timestampValue 时间戳字符串
     * @return 时间戳
     */
    public long parseTimestamp(String timestampValue) {
        try {
            return Long.parseLong(timestampValue);
        } catch (NumberFormatException e) {
            throw new SignException("timestamp 格式非法：" + timestampValue, SignErrorCode.CODE_12204);
        }
    }

    /**
     * 判断：参数列表中的 nonce、timestamp、sign 是否均为合法的
     *
     * @param paramMap 待校验的请求参数集合
     * @param app      接入方配置
     * @return 是否合法
     */
    public boolean isValidParamMap(Map<String, String> paramMap, SignApp app) {
        // 获取必须的三个参数
        String timestampValue = paramMap.get(timestamp);
        String nonceValue = paramMap.get(nonce);
        String signValue = paramMap.get(sign);

        // 参数非空校验
        if (SignUtil.isEmpty(timestampValue) || SignUtil.isEmpty(signValue)) {
            return false;
        }

        // timestamp 非数字直接视为非法，不抛异常
        long timestampLong;
        try {
            timestampLong = Long.parseLong(timestampValue);
        } catch (NumberFormatException e) {
            return false;
        }

        // 三个值的校验必须全部通过
        return isValidTimestamp(timestampLong, app)
                && isValidNonce(nonceValue, app)
                && isValidSign(paramMap, signValue, app);
    }

    /**
     * 校验：参数列表中的 nonce、timestamp、sign 是否均为合法的，如果不合法，则抛出对应的异常
     *
     * @param paramMap 待校验的请求参数集合
     * @param app      接入方配置
     */
    public void checkParamMap(Map<String, String> paramMap, SignApp app) {
        // 获取必须的三个参数
        String timestampValue = paramMap.get(timestamp);
        String nonceValue = paramMap.get(nonce);
        String signValue = paramMap.get(sign);

        // 参数非空校验
        SignException.notEmpty(timestampValue, "缺少 timestamp 字段");
        SignException.notEmpty(nonceValue, "缺少 nonce 字段");
        SignException.notEmpty(signValue, "缺少 sign 字段");

        // 依次校验三个参数
        checkTimestamp(parseTimestamp(timestampValue), app);
        checkNonce(nonceValue, app);
        checkSign(paramMap, signValue, app);

        // 通过
    }

    // ------------------- 返回相应key -------------------

    /**
     * 拼接key：存储 nonce 时使用的 key
     *
     * key 中带 appId，保证不同接入方的 nonce 空间相互隔离，
     * 避免两个第三方恰好生成相同 nonce 时互相干扰
     *
     * @param nonce nonce 值
     * @param app   接入方配置
     * @return nonce 存储 key
     */
    public String splicingNonceSaveKey(String nonce, SignApp app) {
        return "api-sign:nonce:" + app.getAppId() + ":" + nonce;
    }
}
