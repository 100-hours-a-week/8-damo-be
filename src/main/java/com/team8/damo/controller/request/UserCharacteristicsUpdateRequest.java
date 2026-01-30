package com.team8.damo.controller.request;

import com.team8.damo.entity.enumeration.AllergyType;
import com.team8.damo.entity.enumeration.FoodType;
import com.team8.damo.entity.enumeration.IngredientType;
import com.team8.damo.service.request.UserCharacteristicsUpdateServiceRequest;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
public class UserCharacteristicsUpdateRequest {

    private List<AllergyType> allergies;

    @NotEmpty(message = "선호음식은 필수 입력 사항입니다.")
    private List<FoodType> likeFoods;

    @NotEmpty(message = "선호식재료는 필수 입력 사항입니다.")
    private List<IngredientType> likeIngredients;

    @Size(max = 100, message = "추가 특성 정보는 최대 100자 까지 작성 가능합니다.")
    private String otherCharacteristics;

    public UserCharacteristicsUpdateServiceRequest toServiceRequest() {
        return new UserCharacteristicsUpdateServiceRequest(
            allergies,
            likeFoods,
            likeIngredients,
            otherCharacteristics
        );
    }
}
