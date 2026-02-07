package com.team8.damo.service.response;

import com.team8.damo.entity.Group;

public record GroupDetailResponse(
    String name,
    String introduction,
    int participantsCount,
    boolean isGroupLeader
) {
    public static GroupDetailResponse of(Group group, boolean isGroupLeader) {
        return new GroupDetailResponse(
            group.getName(),
            group.getIntroduction(),
            group.getTotalMembers(),
            isGroupLeader
        );
    }
}
