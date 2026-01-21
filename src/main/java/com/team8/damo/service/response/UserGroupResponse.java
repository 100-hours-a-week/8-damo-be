package com.team8.damo.service.response;

import com.team8.damo.entity.UserGroup;

public record UserGroupResponse(
    Long groupId,
    String name,
    String introduction
) {
    public static UserGroupResponse of(UserGroup userGroup) {
        return new UserGroupResponse(
            userGroup.getGroup().getId(),
            userGroup.getGroup().getName(),
            userGroup.getGroup().getIntroduction()
        );
    }
}