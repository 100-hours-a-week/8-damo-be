package com.team8.damo.redis.key;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public enum RedisKeyPrefix {
    LIGHTNING_SUBSCRIBE_USERS("lightning:subscribe:users:"),
    STOMP_SUBSCRIPTION("lightning:subscription:"),
    STOMP_SESSION_SUBSCRIPTIONS("lightning:session:subscriptions:"),
    STOMP_SESSION_USER("lightning:session:user:"),
    DINING_RECOMMENDATION_STREAMING("dining:recommendation:streaming:"),
    ;

    private final String prefix;

    public String key(Object... parts) {
        if (parts == null || parts.length == 0) return prefix;

        return prefix + Arrays.stream(parts)
            .map(String::valueOf)
            .collect(Collectors.joining(":"));
    }
}
