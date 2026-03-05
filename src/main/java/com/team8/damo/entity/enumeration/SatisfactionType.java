package com.team8.damo.entity.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SatisfactionType {
    DELICIOUS_FOOD("맛있는 음식"),
    ENJOYABLE_ATMOSPHERE("즐거운 분위기"),
    KIND_SERVICE("친절한 서비스"),
    OPTIMAL_LOCATION("최적 위치"),
    GROUP_COMPATIBILITY("그룹 궁합도"),
    DINING_SUCCESS("회식 성공도");

    private final String description;
}
