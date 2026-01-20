package com.team8.damo.controller.response;

import com.team8.damo.entity.enumeration.OnboardingStep;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OAuthLoginResponse {
    private Long userId;
    private OnboardingStep onboardingStep;
}
