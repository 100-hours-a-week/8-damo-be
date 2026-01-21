package com.team8.damo.service.response;

import com.team8.damo.entity.User;
import com.team8.damo.entity.enumeration.AgeGroup;
import com.team8.damo.entity.enumeration.Gender;
import lombok.Getter;

@Getter
public class UserBasicResponse {
    private final Long userId;
    private final String nickname;
    private final Gender gender;
    private final AgeGroup ageGroup;

    private UserBasicResponse(Long userId, String nickname, Gender gender, AgeGroup ageGroup) {
        this.userId = userId;
        this.nickname = nickname;
        this.gender = gender;
        this.ageGroup = ageGroup;
    }

    public static UserBasicResponse from(User user) {
        return new UserBasicResponse(
            user.getId(),
            user.getNickname(),
            user.getGender(),
            user.getAgeGroup()
        );
    }
}
