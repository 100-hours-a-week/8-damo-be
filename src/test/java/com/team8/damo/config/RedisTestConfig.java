package com.team8.damo.config;

import io.lettuce.core.api.StatefulConnection;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.CacheKeyPrefix;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@TestConfiguration
public class RedisTestConfig {

    @Bean
    @Primary
    public RedisConnectionFactory testRedisConnectionFactory() {
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration("127.0.0.1", 6380);

        GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(8);
        poolConfig.setMaxIdle(8);
        poolConfig.setMinIdle(2);
        poolConfig.setMaxWait(Duration.ofMillis(-1));
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);

        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
            .poolConfig(poolConfig)
            .commandTimeout(Duration.ofSeconds(2))
            .build();

        return new LettuceConnectionFactory(serverConfig, clientConfig);
    }

    @Bean
    @Primary
    public JacksonJsonRedisSerializer<Object> testJacksonJsonRedisSerializer() {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator
            .builder()
            .allowIfSubType(Object.class)
            .build();

        ObjectMapper objectMapper = JsonMapper.builder()
            .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
            .build();

        return new JacksonJsonRedisSerializer<>(objectMapper, Object.class);
    }

    @Bean
    @Primary
    public RedisTemplate<String, String> testRedisTemplate(
        RedisConnectionFactory testRedisConnectionFactory,
        JacksonJsonRedisSerializer<Object> testJacksonJsonRedisSerializer
    ) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(testRedisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(testJacksonJsonRedisSerializer);
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(testJacksonJsonRedisSerializer);
        return redisTemplate;
    }

    @Bean
    @Primary
    public CacheManager testCacheManager(
        RedisConnectionFactory testRedisConnectionFactory,
        JacksonJsonRedisSerializer<Object> testJacksonJsonRedisSerializer
    ) {
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues()
            .entryTtl(Duration.ofHours(1L))
            .computePrefixWith(CacheKeyPrefix.simple())
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(testJacksonJsonRedisSerializer));

        Map<String, RedisCacheConfiguration> redisCacheConfigurationMap = new HashMap<>();

        return RedisCacheManager.RedisCacheManagerBuilder
            .fromConnectionFactory(testRedisConnectionFactory)
            .cacheDefaults(redisCacheConfiguration)
            .withInitialCacheConfigurations(redisCacheConfigurationMap)
            .enableStatistics()
            .build();
    }
}
