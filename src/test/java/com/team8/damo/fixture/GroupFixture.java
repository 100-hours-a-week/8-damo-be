package com.team8.damo.fixture;

import com.team8.damo.entity.Group;

public class GroupFixture {

    public static Group create(Long id) {
        return Group.builder()
            .id(id)
            .name("테스트그룹")
            .introduction("테스트 소개글")
            .latitude(37.5665)
            .longitude(126.9780)
            .build();
    }

    public static Group create(Long id, String name) {
        return Group.builder()
            .id(id)
            .name(name)
            .introduction("테스트 소개글")
            .latitude(37.5665)
            .longitude(126.9780)
            .build();
    }

    public static Group create(Long id, String name, String introduction) {
        return Group.builder()
            .id(id)
            .name(name)
            .introduction(introduction)
            .latitude(37.5665)
            .longitude(126.9780)
            .build();
    }

    public static Group create(Long id, String name, String introduction, int totalMembers) {
        return Group.builder()
            .id(id)
            .name(name)
            .introduction(introduction)
            .latitude(37.5665)
            .longitude(126.9780)
            .totalMembers(totalMembers)
            .build();
    }
}
