package com.team8.damo.service.request;

import com.team8.damo.entity.enumeration.AllergyType;
import com.team8.damo.entity.enumeration.FoodType;
import com.team8.damo.entity.enumeration.IngredientType;

import java.util.List;

public record UserCharacteristicsCreateServiceRequest(
    List<AllergyType> allergies,
    List<FoodType> likeFoods,
    List<IngredientType> likeIngredients,
    String otherCharacteristics
) {}
