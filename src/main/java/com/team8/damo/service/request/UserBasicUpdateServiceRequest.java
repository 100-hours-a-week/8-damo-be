package com.team8.damo.service.request;

import com.team8.damo.entity.enumeration.AgeGroup;
import com.team8.damo.entity.enumeration.Gender;

public record UserBasicUpdateServiceRequest(
    String nickname,
    Gender gender,
    AgeGroup ageGroup
) {}
