package com.team8.damo.security.handler;

import com.team8.damo.service.LightningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

import static com.team8.damo.redis.key.RedisKeyPrefix.LIGHTNING_SUBSCRIBE_USERS;
import static com.team8.damo.redis.key.RedisKeyPrefix.STOMP_SESSION_SUBSCRIPTIONS;
import static com.team8.damo.redis.key.RedisKeyPrefix.STOMP_SESSION_USER;
import static com.team8.damo.redis.key.RedisKeyPrefix.STOMP_SUBSCRIPTION;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompSubscriptionCleanupManager {

    private final RedisTemplate<String, String> redisTemplate;
    private final LightningService lightningService;

    public void registerSubscription(Long userId, String sessionId, String subscriptionId, Long lightningId) {
        if (userId == null || sessionId == null || subscriptionId == null || lightningId == null) {
            log.warn("Skip register subscription. userId={}, sessionId={}, subscriptionId={}, lightningId={}",
                userId, sessionId, subscriptionId, lightningId);
            return;
        }

        redisTemplate.opsForValue().set(
            STOMP_SUBSCRIPTION.key(sessionId, subscriptionId), lightningId.toString()
        );
        redisTemplate.opsForSet().add(STOMP_SESSION_SUBSCRIPTIONS.key(sessionId), subscriptionId);
        redisTemplate.opsForValue().setIfAbsent(STOMP_SESSION_USER.key(sessionId), userId.toString());
        redisTemplate.opsForSet().add(
            LIGHTNING_SUBSCRIBE_USERS.key(lightningId), userId.toString()
        );

        lightningService.onSubscribe(userId, lightningId);
    }

    public void unregisterSubscription(Long userId, String sessionId, String subscriptionId) {
        if (sessionId == null || subscriptionId == null) {
            return;
        }

        String subscriptionKey = STOMP_SUBSCRIPTION.key(sessionId, subscriptionId);
        String lightningIdStr = redisTemplate.opsForValue().get(subscriptionKey);
        Boolean deleted = redisTemplate.delete(subscriptionKey);
        redisTemplate.opsForSet().remove(STOMP_SESSION_SUBSCRIPTIONS.key(sessionId), subscriptionId);

        if (!Boolean.TRUE.equals(deleted)) {
            cleanupSessionMetadataIfNeeded(sessionId);
            return;
        }

        Long resolvedUserId = resolveUserId(userId, sessionId);
        Long lightningId = parseLongOrNull(lightningIdStr, subscriptionKey);
        if (lightningId != null && resolvedUserId != null) {
            redisTemplate.opsForSet().remove(
                LIGHTNING_SUBSCRIBE_USERS.key(lightningId),
                resolvedUserId.toString()
            );
            lightningService.onUnsubscribe(resolvedUserId, lightningId);
        }

        cleanupSessionMetadataIfNeeded(sessionId);
    }

    public void unregisterAllBySession(Long userId, String sessionId) {
        if (sessionId == null) {
            return;
        }

        Set<String> subscriptionIds = redisTemplate.opsForSet().members(
            STOMP_SESSION_SUBSCRIPTIONS.key(sessionId)
        );
        if (subscriptionIds == null || subscriptionIds.isEmpty()) {
            cleanupSessionMetadataIfNeeded(sessionId);
            return;
        }

        for (String subscriptionId : subscriptionIds) {
            unregisterSubscription(userId, sessionId, subscriptionId);
        }
        cleanupSessionMetadataIfNeeded(sessionId);
    }

    private Long resolveUserId(Long userId, String sessionId) {
        if (userId != null) {
            return userId;
        }

        String userIdStr = redisTemplate.opsForValue().get(STOMP_SESSION_USER.key(sessionId));
        return parseLongOrNull(userIdStr, STOMP_SESSION_USER.key(sessionId));
    }

    private void cleanupSessionMetadataIfNeeded(String sessionId) {
        String sessionSubscriptionsKey = STOMP_SESSION_SUBSCRIPTIONS.key(sessionId);
        Long size = redisTemplate.opsForSet().size(sessionSubscriptionsKey);

        if (size == null || size == 0) {
            redisTemplate.delete(sessionSubscriptionsKey);
            redisTemplate.delete(STOMP_SESSION_USER.key(sessionId));
        }
    }

    private Long parseLongOrNull(String value, String key) {
        if (value == null) {
            return null;
        }

        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid number value in redis. key={}, value={}", key, value);
            return null;
        }
    }
}
