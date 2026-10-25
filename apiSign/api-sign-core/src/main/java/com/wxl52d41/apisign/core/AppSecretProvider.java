package com.wxl52d41.apisign.core;

/**
 * 接入方密钥提供者
 *
 * 默认由配置（YAML）提供，也可实现本接口从数据库 / 配置中心加载，
 * 注册为 Spring Bean 即可覆盖默认实现。实现方应考虑缓存，避免每次请求都查库。
 *
 * @author wxl52d41
 * @since 1.0.0
 */
public interface AppSecretProvider {

    /**
     * 根据 appId 获取接入方配置
     *
     * @param appId 接入方标识
     * @return 接入方配置，未登记时返回 null
     */
    SignApp getApp(String appId);
}
