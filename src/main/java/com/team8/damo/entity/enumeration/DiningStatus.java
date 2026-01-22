package com.team8.damo.entity.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DiningStatus {
    ATTENDANCE_VOTING("참석 투표 중"),
    RESTAURANT_VOTING("식당 투표 중"),
    CONFIRMED("회식 확정"),
    COMPLETE("회식 완료");

    private final String description;
}
