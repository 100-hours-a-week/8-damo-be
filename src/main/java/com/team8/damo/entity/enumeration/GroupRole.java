package com.team8.damo.entity.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GroupRole {
    LEADER("그룹장"),
    PARTICIPANT("참석자");

    private final String description;
}
