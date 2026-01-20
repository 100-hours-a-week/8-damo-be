package com.team8.damo.service.response;

import com.team8.damo.entity.enumeration.OnboardingStep;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserOAuthResponse {
    private Boolean isNew;
    private Long userId;
    private OnboardingStep onboardingStep;
    private String accessToken;
    private String refreshToken;
}
