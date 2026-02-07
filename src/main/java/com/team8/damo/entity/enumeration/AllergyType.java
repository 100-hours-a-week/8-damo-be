package com.team8.damo.entity.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AllergyType {
    SHRIMP("새우"),
    OYSTER("굴"),
    CRAB("게"),
    MUSSEL("홍합"),
    SQUID("오징어"),
    ABALONE("전복"),
    MACKEREL("고등어"),
    SHELLFISH("조개류"),
    BUCKWHEAT("메밀"),
    WHEAT("밀"),
    SOYBEAN("대두"),
    WALNUT("호두"),
    PEANUT("땅콩"),
    PINE_NUT("잣"),
    EGG("알류(가금류)"),
    MILK("우유"),
    BEEF("쇠고기"),
    PORK("돼지고기"),
    CHICKEN("닭고기"),
    PEACH("복숭아"),
    TOMATO("토마토"),
    SULFITES("아황산류");

    private final String description;
}

