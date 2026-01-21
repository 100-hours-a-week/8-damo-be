package com.team8.damo.service.request;

import java.util.List;

public record UserCharacteristicsCreateServiceRequest(
    List<Integer> allergyIds,
    List<Integer> likeFoodIds,
    List<Integer> likeIngredientIds,
    String otherCharacteristics
) {}
