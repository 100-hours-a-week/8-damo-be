package com.team8.damo.service.response;

import com.team8.damo.entity.AllergyCategory;
import com.team8.damo.entity.LikeFoodCategory;
import com.team8.damo.entity.LikeIngredientCategory;
import com.team8.damo.entity.User;
import lombok.Getter;

import java.util.List;
import java.util.function.Function;

@Getter
public class UserProfileResponse {
    private final Long userId;
    private final String nickname;
    private final List<CategoryResponse> allergies;
    private final List<CategoryResponse> likeFoods;
    private final List<CategoryResponse> likeIngredients;
    private final String otherCharacteristics;

    private UserProfileResponse(
        Long userId,
        String nickname,
        List<CategoryResponse> allergies,
        List<CategoryResponse> likeFoods,
        List<CategoryResponse> likeIngredients,
        String otherCharacteristics
    ) {
        this.userId = userId;
        this.nickname = nickname;
        this.allergies = allergies;
        this.likeFoods = likeFoods;
        this.likeIngredients = likeIngredients;
        this.otherCharacteristics = otherCharacteristics;
    }

    public static UserProfileResponse of(
        User user,
        List<AllergyCategory> allergies,
        List<LikeFoodCategory> likeFoods,
        List<LikeIngredientCategory> likeIngredients
    ) {
        List<CategoryResponse> allergyResponses = toCategoryResponses(
            allergies,
            AllergyCategory::getId,
            allergy -> allergy.getCategory().getDescription()
        );
        List<CategoryResponse> likeFoodResponses = toCategoryResponses(
            likeFoods,
            LikeFoodCategory::getId,
            likeFood -> likeFood.getCategory().getDescription()
        );
        List<CategoryResponse> likeIngredientResponses = toCategoryResponses(
            likeIngredients,
            LikeIngredientCategory::getId,
            likeIngredient -> likeIngredient.getCategory().getDescription()
        );

        return new UserProfileResponse(
            user.getId(),
            user.getNickname(),
            allergyResponses,
            likeFoodResponses,
            likeIngredientResponses,
            user.getOtherCharacteristics()
        );
    }

    private static <T> List<CategoryResponse> toCategoryResponses(
        List<T> categories,
        Function<T, Integer> idExtractor,
        Function<T, String> descriptionExtractor
    ) {
        return categories.stream()
            .map(category -> CategoryResponse.from(
                idExtractor.apply(category),
                descriptionExtractor.apply(category)
            ))
            .toList();
    }
}
