package com.team8.damo.entity.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AllergyType {
    SHRIMP("새우"),
    CRAB("게"),
    SQUID("오징어"),
    MACKEREL("고등어"),
    SHELLFISH("조개류"),
    EGG("달걀"),
    MILK("우유"),
    BUCKWHEAT("메밀"),
    WHEAT("밀"),
    PEANUT("땅콩"),
    SOY("대두"),
    WALNUT("호두"),
    PINE_NUT("잣"),
    ALMOND("아몬드"),
    PEACH("복숭아"),
    TOMATO("토마토"),
    PORK("돼지고기"),
    BEEF("쇠고기"),
    CHICKEN("닭고기"),
    SULFITE("아황산류"),
    SESAME("참깨"),
    NONE("없음");

    private final String description;
}
