package com.team8.damo.entity.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GatheringRole {
    LEADER("모임장"),
    PARTICIPANT("참석자");

    private final String description;
}
