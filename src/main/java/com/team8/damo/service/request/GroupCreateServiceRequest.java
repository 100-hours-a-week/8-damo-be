package com.team8.damo.service.request;

import com.team8.damo.entity.Group;

public record GroupCreateServiceRequest(
    String name,
    String introduction,
    double latitude,
    double longitude,
    String imagePath
) {
    public Group toEntity(Long id) {
        return Group.builder()
            .id(id)
            .name(name)
            .introduction(introduction)
            .latitude(latitude)
            .longitude(longitude)
            .imagePath(imagePath)
            .build();
    }
}
