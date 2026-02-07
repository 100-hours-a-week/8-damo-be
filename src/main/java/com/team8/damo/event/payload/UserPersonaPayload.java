package com.team8.damo.event.payload;

import com.team8.damo.entity.User;
import com.team8.damo.entity.enumeration.AllergyType;
import com.team8.damo.entity.enumeration.FoodType;
import com.team8.damo.entity.enumeration.IngredientType;
import lombok.Builder;

import java.util.List;

@Builder
public record UserPersonaPayload(
    User user,
    List<AllergyType> allergies,
    List<FoodType> likeFoods,
    List<IngredientType> likeIngredients
) implements EventPayload {

}
