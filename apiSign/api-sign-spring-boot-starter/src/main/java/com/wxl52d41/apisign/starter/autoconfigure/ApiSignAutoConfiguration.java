package com.wxl52d41.apisign.starter.autoconfigure;

import com.wxl52d41.apisign.core.AppSecretProvider;
import com.wxl52d41.apisign.core.MemoryNonceStore;
import com.wxl52d41.apisign.core.NonceStore;
import com.wxl52d41.apisign.core.SignTemplate;
import com.wxl52d41.apisign.starter.annotation.ApiSignInterceptor;
import com.wxl52d41.apisign.starter.app.PropertiesAppSecretProvider;
import com.wxl52d41.apisign.starter.web.RepeatableReadFilter;
import com.wxl52d41.apisign.starter.web.SignParamResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API 签名验签自动装配
 *
 * 引入依赖即在 Web 应用中生效，无需额外注解。
 * 可通过 api.sign.enabled=false 关闭；各 Bean 均可由宿主自定义覆盖。
 *
 * @author wxl52d41
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "api.sign", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(SignProperties.class)
public class ApiSignAutoConfiguration implements WebMvcConfigurer {

    private final SignProperties signProperties;

    /**
     * 用 ObjectProvider 延迟解析，避免配置类构造期与本类 @Bean 方法形成循环依赖
     */
    private final ObjectProvider<ApiSignInterceptor> interceptorProvider;

    public ApiSignAutoConfiguration(SignProperties signProperties,
                                    ObjectProvider<ApiSignInterceptor> interceptorProvider) {
        this.signProperties = signProperties;
        this.interceptorProvider = interceptorProvider;
    }

    /**
     * 接入方密钥提供者，默认读 YAML；需要从数据库加载时注册自己的 AppSecretProvider Bean 即可覆盖
     */
    @Bean
    @ConditionalOnMissingBean(AppSecretProvider.class)
    public AppSecretProvider appSecretProvider() {
        return new PropertiesAppSecretProvider(signProperties.getApps());
    }

    /**
     * nonce 防重放存储，默认内存实现；多实例部署请用 Redis 实现覆盖
     */
    @Bean
    @ConditionalOnMissingBean(NonceStore.class)
    public NonceStore nonceStore() {
        return new MemoryNonceStore();
    }

    /*
     * ==========================================================================
     * 【启用 Redis 版 nonce 存储】—— 当前为注释状态，默认使用上面的 MemoryNonceStore
     *
     * 前置条件：
     *   1. 宿主项目已引入 spring-boot-starter-data-redis
     *      （本 starter 已 optional 引入，宿主引入即生效）
     *   2. 宿主配置了 spring.redis.host / port / password（或 spring.data.redis.*，
     *      视 Boot 版本而定）
     *
     * 启用方式：把下面这个 Bean 的注释去掉。
     * 它带 @ConditionalOnClass + @ConditionalOnBean，宿主没有 Redis 时自动跳过，
     * 不会因为缺 StringRedisTemplate 导致启动失败。
     *
     * 为什么不直接改 nonceStore()：保留它作为兜底，Redis 环境不可用时行为可控，
     * 且 @ConditionalOnMissingBean 保证两者不会同时生效。
     *
     * 取消注释时需要同时补充以下 import：
     *   import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
     *   import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
     *   import org.springframework.data.redis.core.StringRedisTemplate;
     *   import com.wxl52d41.apisign.starter.store.RedisNonceStore;
     * ==========================================================================
     */
    // @Bean
    // @ConditionalOnClass({StringRedisTemplate.class})
    // @ConditionalOnBean({StringRedisTemplate.class})
    // @ConditionalOnMissingBean(NonceStore.class)
    // public NonceStore redisNonceStore(StringRedisTemplate stringRedisTemplate) {
    //     return new RedisNonceStore(stringRedisTemplate);
    // }

    /**
     * 签名算法模板（无状态，可安全复用）
     */
    @Bean
    @ConditionalOnMissingBean(SignTemplate.class)
    public SignTemplate signTemplate(NonceStore nonceStore) {
        return new SignTemplate(nonceStore);
    }

    /**
     * 签名参数解析器
     */
    @Bean
    @ConditionalOnMissingBean(SignParamResolver.class)
    public SignParamResolver signParamResolver() {
        return new SignParamResolver();
    }

    /**
     * ApiSign 注解拦截器
     */
    @Bean
    @ConditionalOnMissingBean(ApiSignInterceptor.class)
    public ApiSignInterceptor apiSignInterceptor(SignTemplate signTemplate,
                                                 SignParamResolver signParamResolver,
                                                 AppSecretProvider appSecretProvider) {
        return new ApiSignInterceptor(signTemplate, signParamResolver, appSecretProvider,
                signProperties.getAppIdHeader());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptorProvider.getObject()).addPathPatterns("/**");
    }

    /**
     * JSON body 可重复读过滤器
     */
    @Bean
    public FilterRegistrationBean<RepeatableReadFilter> apiSignRepeatableReadFilter() {
        FilterRegistrationBean<RepeatableReadFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RepeatableReadFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(signProperties.getFilterOrder() == 0
                ? Ordered.HIGHEST_PRECEDENCE
                : signProperties.getFilterOrder());
        return registrationBean;
    }
}
