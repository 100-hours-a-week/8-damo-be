package com.team8.damo.event;

import com.team8.damo.entity.User;
import com.team8.damo.entity.enumeration.AllergyType;
import com.team8.damo.entity.enumeration.FoodType;
import com.team8.damo.entity.enumeration.IngredientType;

import java.util.List;

public record UserPersonaEvent(
    User user,
    List<AllergyType> allergies,
    List<FoodType> likeFoods,
    List<IngredientType> likeIngredients
) {
    public static UserPersonaEvent of(
        User user,
        List<AllergyType> allergies,
        List<FoodType> likeFoods,
        List<IngredientType> likeIngredients
    ) {
        return new UserPersonaEvent(user, allergies, likeFoods, likeIngredients);
    }
}
