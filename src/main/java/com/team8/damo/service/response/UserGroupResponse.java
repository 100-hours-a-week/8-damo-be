package com.team8.damo.service.response;

import com.team8.damo.entity.UserGroup;

public record UserGroupResponse(
    Long groupId,
    String name,
    String introduction,
    String imagePath
) {
    public static UserGroupResponse from(UserGroup userGroup) {
        return new UserGroupResponse(
            userGroup.getGroup().getId(),
            userGroup.getGroup().getName(),
            userGroup.getGroup().getIntroduction(),
            userGroup.getGroup().getImagePath()
        );
    }
}