package com.wxl52d41.apikey.starter.autoconfigure;

import com.wxl52d41.apikey.core.ApiKeyStore;
import com.wxl52d41.apikey.core.ApiKeyTemplate;
import com.wxl52d41.apikey.starter.annotation.ApiKeyCheckInterceptor;
import com.wxl52d41.apikey.starter.store.RedisApiKeyStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API Key 自动装配
 *
 * 引入依赖即在 Web 应用中生效。存储固定使用 Redis 实现 {@link RedisApiKeyStore}
 * （宿主需引入 spring-boot-starter-data-redis 并配置连接），多实例天然共享数据。
 * 需要其他存储（DB 等）时注册自定义 ApiKeyStore Bean 覆盖。
 *
 * @author wxl52d41
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "api.key", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(ApiKeyProperties.class)
public class ApiKeyAutoConfiguration implements WebMvcConfigurer {

    private final ApiKeyProperties apiKeyProperties;

    /**
     * 用 ObjectProvider 延迟解析，避免配置类构造期与本类 @Bean 方法形成循环依赖
     */
    private final ObjectProvider<ApiKeyCheckInterceptor> interceptorProvider;

    public ApiKeyAutoConfiguration(ApiKeyProperties apiKeyProperties,
                                   ObjectProvider<ApiKeyCheckInterceptor> interceptorProvider) {
        this.apiKeyProperties = apiKeyProperties;
        this.interceptorProvider = interceptorProvider;
    }

    /**
     * ApiKey 存储：Redis 实现（唯一默认实现，多实例共享）
     *
     * 宿主需引入 spring-boot-starter-data-redis 并配置 spring.redis.* 连接信息
     */
    @Bean
    @ConditionalOnMissingBean(ApiKeyStore.class)
    public ApiKeyStore apiKeyStore(StringRedisTemplate stringRedisTemplate) {
        return new RedisApiKeyStore(stringRedisTemplate);
    }

    /**
     * ApiKey 操作模板（无状态，可安全复用）
     */
    @Bean
    @ConditionalOnMissingBean(ApiKeyTemplate.class)
    public ApiKeyTemplate apiKeyTemplate(ApiKeyStore apiKeyStore) {
        return new ApiKeyTemplate(apiKeyStore, apiKeyProperties.toApiKeyConfig());
    }

    /**
     * ApiKeyCheck 注解拦截器
     */
    @Bean
    @ConditionalOnMissingBean(ApiKeyCheckInterceptor.class)
    public ApiKeyCheckInterceptor apiKeyCheckInterceptor(ApiKeyTemplate apiKeyTemplate) {
        return new ApiKeyCheckInterceptor(apiKeyTemplate);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptorProvider.getObject()).addPathPatterns("/**");
    }
}
