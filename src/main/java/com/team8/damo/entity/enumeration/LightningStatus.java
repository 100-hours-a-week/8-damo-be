package com.team8.damo.entity.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LightningStatus {

    OPEN("모집 중"),
    CLOSED("마감"),
    DELETED("삭제");

    private final String description;
}
