package com.autohubstore.catalogservice.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RedisConfig {

    public static final String CACHE_PRODUCTS = "products";
    public static final String CACHE_PRODUCTS_BY_CATEGORY = "products-by-category";

    private static final int PRODUCT_TTL_MINUTES = 5;
    private static final int PRODUCTS_BY_CATEGORY_TTL_MINUTES = 2;

    private static final Duration PRODUCT_TTL = Duration.ofMinutes(PRODUCT_TTL_MINUTES);
    private static final Duration PRODUCTS_BY_CATEGORY_TTL = Duration.ofMinutes(PRODUCTS_BY_CATEGORY_TTL_MINUTES);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper redisObjectMapper = new ObjectMapper();
        redisObjectMapper.findAndRegisterModules();
        redisObjectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(PRODUCT_TTL)
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper)));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(CACHE_PRODUCTS, defaultConfig.entryTtl(PRODUCT_TTL));
        cacheConfigurations.put(CACHE_PRODUCTS_BY_CATEGORY, defaultConfig.entryTtl(PRODUCTS_BY_CATEGORY_TTL));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

}
