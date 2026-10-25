package com.wxl52d41.apisign.core;

import java.security.SecureRandom;

/**
 * 签名验签模块工具类
 *
 * 拷贝自 sa-token 的 SaFoxUtil（最小集）
 *
 * @author click33 / wxl52d41
 * @since 1.0.0
 */
public class SignUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 指定元素是否为 (null或者空字符串)
     *
     * @param str 指定元素
     * @return 是否为null或者空字符串
     */
    public static boolean isEmpty(Object str) {
        return str == null || "".equals(str);
    }

    /**
     * 指定元素是否不为 (null或者空字符串)
     *
     * @param str 指定元素
     * @return 是否为null或者空字符串
     */
    public static boolean isNotEmpty(Object str) {
        return !isEmpty(str);
    }

    /**
     * 生成指定长度的随机字符串
     *
     * @param length 长度
     * @return 随机字符串
     */
    public static String getRandomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int number = SECURE_RANDOM.nextInt(62);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }
}
