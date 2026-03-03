package com.team8.damo.cache.dto;


import com.team8.damo.entity.User;

public record UserBasicCache(
    Long id,
    String email,
    String nickname,
    String fcmToken,
    String imagePath
) {

    public static UserBasicCache from(User user) {
        return new UserBasicCache(
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            user.getFcmToken(),
            user.getImagePath()
        );
    }
}
