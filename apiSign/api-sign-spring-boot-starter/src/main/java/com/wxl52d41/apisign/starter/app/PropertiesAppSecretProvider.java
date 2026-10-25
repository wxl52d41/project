package com.wxl52d41.apisign.starter.app;

import com.wxl52d41.apisign.core.AppSecretProvider;
import com.wxl52d41.apisign.core.SignApp;
import com.wxl52d41.apisign.core.SignException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 从 YAML 配置加载接入方密钥的默认实现
 *
 * 启动时把所有 appId 构建为只读 Map，运行期零查询开销；
 * 需要从数据库 / 配置中心动态加载时，注册自定义 AppSecretProvider Bean 即可覆盖本实现。
 *
 * @author wxl52d41
 * @since 1.0.0
 */
public class PropertiesAppSecretProvider implements AppSecretProvider {

    private final Map<String, SignApp> appMap = new HashMap<>();

    public PropertiesAppSecretProvider(List<SignApp> apps) {
        if (apps != null) {
            for (SignApp app : apps) {
                if (app == null || app.getAppId() == null || app.getAppId().isEmpty()) {
                    throw new SignException("api.sign.apps 中存在 appId 为空的配置项");
                }
                if (appMap.containsKey(app.getAppId())) {
                    throw new SignException("api.sign.apps 中 appId 重复：" + app.getAppId());
                }
                appMap.put(app.getAppId(), app);
            }
        }
    }

    @Override
    public SignApp getApp(String appId) {
        return appId == null ? null : appMap.get(appId);
    }
}
