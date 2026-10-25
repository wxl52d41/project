package com.wxl52d41.apisign.core;

/**
 * 签名验签模块异常细分状态码
 *
 * 拷贝自 sa-token 的 SaSignErrorCode，值保持一致
 *
 * @author click33 / wxl52d41
 * @since 1.0.0
 */
public interface SignErrorCode {

    /** 参与参数签名的秘钥不可为空 */
    int CODE_12201 = 12201;

    /** 给定的签名无效 */
    int CODE_12202 = 12202;

    /** timestamp 超出允许的范围 */
    int CODE_12203 = 12203;

    /** 请求缺少必要的签名参数（timestamp / nonce / sign） */
    int CODE_12204 = 12204;

    /** 不支持的摘要算法 */
    int CODE_12205 = 12205;

    /** 读取或解析请求体失败 */
    int CODE_12206 = 12206;

    /** 缺少接入方标识 appId */
    int CODE_12207 = 12207;

    /** 未登记的 appId */
    int CODE_12208 = 12208;

    /** appId 已停用 */
    int CODE_12209 = 12209;

    /** appId 无权访问该接口 */
    int CODE_12210 = 12210;

}
