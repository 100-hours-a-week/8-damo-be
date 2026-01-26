package com.team8.damo.config;

import io.lettuce.core.api.StatefulConnection;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.CacheKeyPrefix;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
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

@Configuration
@EnableRedisRepositories
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration(host, port);

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
    public JacksonJsonRedisSerializer<Object> jacksonJsonRedisSerializer() {
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
    public RedisTemplate<String, String> redisTemplate(JacksonJsonRedisSerializer<Object> serializer) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(serializer);
        return redisTemplate;
    }

    @Bean(name = "cacheManager")
    public RedisCacheManager cacheManager(
        RedisConnectionFactory redisConnectionFactory,
        JacksonJsonRedisSerializer<Object> serializer
    ) {
        // 캐시의 기본 설정
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues()  // null 캐싱X
            .entryTtl(Duration.ofHours(1L))
            .computePrefixWith(CacheKeyPrefix.simple())  // 캐시 키의 접두어를 간단하게 계산 EX) UserCache::Key 의 형태로 저장
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))  // 키의 직렬화 방식 설정
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));  // 값의 직렬화 방식 설정

        // 캐시 이름 설정 담아주기
        Map<String, RedisCacheConfiguration> redisCacheConfigurationMap = new HashMap<>();
        // redisCacheConfigurationMap.put("user::email", redisCacheConfiguration);

        // RedisCacheManager 리턴
        return RedisCacheManager.RedisCacheManagerBuilder
            .fromConnectionFactory(redisConnectionFactory)
            .cacheDefaults(redisCacheConfiguration)
            .withInitialCacheConfigurations(redisCacheConfigurationMap)
            .enableStatistics()
            .build();
    }


}
