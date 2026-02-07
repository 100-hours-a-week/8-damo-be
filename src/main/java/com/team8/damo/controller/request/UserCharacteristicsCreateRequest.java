package com.team8.damo.controller.request;

import com.team8.damo.entity.enumeration.AllergyType;
import com.team8.damo.entity.enumeration.FoodType;
import com.team8.damo.entity.enumeration.IngredientType;
import com.team8.damo.service.request.UserCharacteristicsCreateServiceRequest;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
public class UserCharacteristicsCreateRequest {

    private List<AllergyType> allergies;
    private List<FoodType> likeFoods;
    private List<IngredientType> likeIngredients;

    @Size(max = 100, message = "추가 특성 정보는 최대 100자 까지 작성 가능합니다.")
    private String otherCharacteristics;

    public UserCharacteristicsCreateServiceRequest toServiceRequest() {
        return new UserCharacteristicsCreateServiceRequest(
            allergies,
            likeFoods,
            likeIngredients,
            otherCharacteristics
        );
    }
}
