package com.team8.damo.service.response;

import com.team8.damo.entity.User;
import com.team8.damo.entity.enumeration.AgeGroup;
import com.team8.damo.entity.enumeration.Gender;
import lombok.Getter;

public record UserBasicResponse(
    Long userId,
    String nickname,
    Gender gender,
    AgeGroup ageGroup,
    String imagePath
) {
    public static UserBasicResponse from(User user) {
        return new UserBasicResponse(
            user.getId(),
            user.getNickname(),
            user.getGender(),
            user.getAgeGroup(),
            user.getImagePath()
        );
    }
}
