package com.team8.damo.client.request;

import com.team8.damo.entity.User;
import com.team8.damo.entity.enumeration.*;

import java.util.List;

public record UserData(
    Long id,
    String nickname,
    Gender gender,
    AgeGroup ageGroup,
    List<AllergyType> allergies,
    List<FoodType> likeFoods,
    List<IngredientType> likeIngredients,
    String otherCharacteristics
) {
    public static UserData of(
        User user, List<AllergyType> allergies,
        List<FoodType> likeFoods, List<IngredientType> likeIngredients
    ) {
        return new UserData(
            user.getId(),
            user.getNickname(),
            user.getGender(),
            user.getAgeGroup(),
            allergies,
            likeFoods,
            likeIngredients,
            user.getOtherCharacteristics()
        );
    }
}
