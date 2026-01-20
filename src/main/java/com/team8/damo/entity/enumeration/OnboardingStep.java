package com.team8.damo.entity.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OnboardingStep {
    BASIC("기본 정보 수집"),
    CHARACTERISTIC("개인 특성 수집"),
    DONE("완료");

    private final String description;
}
