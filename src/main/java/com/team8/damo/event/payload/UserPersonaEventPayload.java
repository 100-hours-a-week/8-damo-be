package com.team8.damo.event.payload;

import com.team8.damo.entity.enumeration.*;
import lombok.Builder;

import java.util.List;

@Builder
public record UserPersonaEventPayload(
    Long userId,
    String nickname,
    Gender gender,
    AgeGroup ageGroup,
    List<AllergyType> allergies,
    List<FoodType> likeFoods,
    List<IngredientType> likeIngredients,
    String otherCharacteristics
) implements EventPayload {
}
