package com.team8.damo.config;

import com.team8.damo.cache.CacheSpec;
import com.team8.damo.chat.producer.RedisMessageBroker;
import io.lettuce.core.api.StatefulConnection;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.CacheKeyPrefix;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@EnableRedisRepositories
public class RedisConfig {

    @Value("${spring.data.redis.host:#{null}}")
    private String host;

    @Value("${spring.data.redis.port:#{null}}")
    private int port;

    @Value("${spring.data.redis.sentinel.master:#{null}}")
    private String sentinelMaster;

    @Value("${spring.data.redis.sentinel.nodes:#{null}}")
    private String sentinelNodes;

    public static final String CHANNEL = "chat:broadcast";

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
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

        if (sentinelMaster != null) {
            // prod: Sentinel 모드
            RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration();
            sentinelConfig.setMaster(sentinelMaster);
            for (String node : sentinelNodes.split(",")) {
                String[] parts = node.trim().split(":");
                sentinelConfig.addSentinel(new RedisNode(parts[0], Integer.parseInt(parts[1])));
            }
            return new LettuceConnectionFactory(sentinelConfig, clientConfig);
        } else {
            // local/test: Standalone 모드 (기존 방식 유지)
            RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration(host, port);
            return new LettuceConnectionFactory(serverConfig, clientConfig);
        }
    }

    @Bean
    public GenericJacksonJsonRedisSerializer jacksonJsonRedisSerializer() {
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("com.team8.damo.cache.dto")
            .build();

        return GenericJacksonJsonRedisSerializer.builder()
            .enableDefaultTyping(ptv)
            .typePropertyName("@class")
            .enableSpringCacheNullValueSupport("@class")
            .build();
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(GenericJacksonJsonRedisSerializer jacksonJsonRedisSerializer) {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(jacksonJsonRedisSerializer);
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(jacksonJsonRedisSerializer);
        return redisTemplate;
    }

    @Bean(name = "cacheManager")
    public RedisCacheManager cacheManager(
        RedisConnectionFactory redisConnectionFactory,
        GenericJacksonJsonRedisSerializer jacksonJsonRedisSerializer
    ) {
        // 캐시의 기본 설정
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
            .disableCachingNullValues()  // null 캐싱X
            .entryTtl(Duration.ofHours(1L))
            .computePrefixWith(CacheKeyPrefix.simple())  // 캐시 키의 접두어를 간단하게 계산 EX) UserCache::Key 의 형태로 저장
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))  // 키의 직렬화 방식 설정
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jacksonJsonRedisSerializer));  // 값의 직렬화 방식 설정

        // 캐시 이름 설정 담아주기
        Map<String, RedisCacheConfiguration> redisCacheConfigurationMap = new HashMap<>();
        for (CacheSpec spec : CacheSpec.values()) {
            redisCacheConfigurationMap.put(spec.name, redisCacheConfiguration.entryTtl(spec.ttl));
        }

        return RedisCacheManager.RedisCacheManagerBuilder
            .fromConnectionFactory(redisConnectionFactory)
            .cacheDefaults(redisCacheConfiguration)
            .withInitialCacheConfigurations(redisCacheConfigurationMap)
            .enableStatistics()
            .build();
    }

    @Bean
    public RedisMessageListenerContainer listenerContainer(
        RedisConnectionFactory connectionFactory,
        MessageListenerAdapter messageListenerAdapter
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(messageListenerAdapter, new ChannelTopic(CHANNEL));

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("redis-listener-");
        executor.setKeepAliveSeconds(60);
        executor.initialize();

        container.setTaskExecutor(executor);
        return container;
    }

    @Bean
    public MessageListenerAdapter messageListenerAdapter(RedisMessageBroker broker) {
        return new MessageListenerAdapter(broker, "onMessage");
    }
}
