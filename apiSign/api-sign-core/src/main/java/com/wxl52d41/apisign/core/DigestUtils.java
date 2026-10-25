package com.wxl52d41.apisign.core;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 摘要算法工具类
 *
 * 拷贝自 sa-token 的 SaSecureUtil（最小集），算法输出保持一致（小写十六进制字符串）。
 *
 * 与 sa-token 的差异：md5 统一使用 UTF-8 编码（sa-token 使用平台默认编码，跨平台部署时中文参数可能不一致），
 * 在 UTF-8 默认编码的系统上两者结果完全一致。
 *
 * @author click33 / wxl52d41
 * @since 1.0.0
 */
public class DigestUtils {

    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * md5 摘要
     *
     * @param str 指定字符串
     * @return 32位小写十六进制字符串
     */
    public static String md5(String str) {
        return digest(str, "MD5");
    }

    /**
     * sha1 摘要
     *
     * @param str 指定字符串
     * @return 40位小写十六进制字符串
     */
    public static String sha1(String str) {
        return digest(str, "SHA-1");
    }

    /**
     * sha256 摘要
     *
     * @param str 指定字符串
     * @return 64位小写十六进制字符串
     */
    public static String sha256(String str) {
        return digest(str, "SHA-256");
    }

    /**
     * sha384 摘要
     *
     * @param str 指定字符串
     * @return 96位小写十六进制字符串
     */
    public static String sha384(String str) {
        return digest(str, "SHA-384");
    }

    /**
     * sha512 摘要
     *
     * @param str 指定字符串
     * @return 128位小写十六进制字符串
     */
    public static String sha512(String str) {
        return digest(str, "SHA-512");
    }

    /**
     * hmac-md5 摘要
     *
     * @param key  秘钥
     * @param data 指定字符串
     * @return 32位小写十六进制字符串
     */
    public static String hmacMd5(String key, String data) {
        return hmacDigest(key, data, "HmacMD5");
    }

    /**
     * hmac-sha256 摘要
     *
     * @param key  秘钥
     * @param data 指定字符串
     * @return 64位小写十六进制字符串
     */
    public static String hmacSha256(String key, String data) {
        return hmacDigest(key, data, "HmacSHA256");
    }

    /**
     * hmac-sha512 摘要
     *
     * @param key  秘钥
     * @param data 指定字符串
     * @return 128位小写十六进制字符串
     */
    public static String hmacSha512(String key, String data) {
        return hmacDigest(key, data, "HmacSHA512");
    }

    /**
     * 通用消息摘要
     *
     * @param str       指定字符串
     * @param algorithm 摘要算法名称
     * @return 小写十六进制字符串
     */
    public static String digest(String str, String algorithm) {
        try {
            str = (str == null ? "" : str);
            MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
            messageDigest.update(str.getBytes(StandardCharsets.UTF_8));
            return toHexString(messageDigest.digest());
        } catch (Exception e) {
            SignException signException = new SignException("摘要计算失败：" + algorithm, SignErrorCode.CODE_12206);
            signException.initCause(e);
            throw signException;
        }
    }

    /**
     * HMAC 摘要公共方法
     *
     * @param key       秘钥
     * @param data      指定字符串
     * @param algorithm 摘要算法名称，如 HmacSHA256、HmacSHA512、HmacMD5
     * @return 小写十六进制字符串
     */
    public static String hmacDigest(String key, String data, String algorithm) {
        try {
            data = (data == null ? "" : data);
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm);
            Mac mac = Mac.getInstance(algorithm);
            mac.init(signingKey);
            return toHexString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            SignException signException = new SignException("摘要计算失败：" + algorithm, SignErrorCode.CODE_12206);
            signException.initCause(e);
            throw signException;
        }
    }

    /**
     * 字节数组转小写十六进制字符串
     *
     * @param bytes 字节数组
     * @return 小写十六进制字符串
     */
    public static String toHexString(byte[] bytes) {
        char[] strA = new char[bytes.length * 2];
        int k = 0;
        for (byte b : bytes) {
            strA[k++] = HEX_DIGITS[b >>> 4 & 0xf];
            strA[k++] = HEX_DIGITS[b & 0xf];
        }
        return new String(strA);
    }
}
