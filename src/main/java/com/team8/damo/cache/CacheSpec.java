package com.team8.damo.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;

@Getter
@RequiredArgsConstructor
public enum CacheSpec {
    USER_BASIC("user:basic", Duration.ofMinutes(15)),
    ;

    public final String name;
    public final Duration ttl;

    public static final String userBasic = "user:basic";
}
