package com.team8.damo.entity.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VotingStatus {
    PENDING("투표 전"),
    NON_ATTEND("불참"),
    ATTEND("참석");

    private final String description;

    public boolean isNotPending() {
        return this != PENDING;
    }
}
