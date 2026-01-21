package com.team8.damo.controller.request;

import com.team8.damo.service.request.UserCharacteristicsCreateServiceRequest;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
public class UserCharacteristicsCreateRequest {

    private List<Integer> allergyIds;
    private List<Integer> likeFoodIds;
    private List<Integer> likeIngredientIds;

    @Size(max = 100, message = "추가 특성 정보는 최대 100자 까지 작성 가능합니다.")
    private String otherCharacteristics;

    public UserCharacteristicsCreateServiceRequest toServiceRequest() {
        return new UserCharacteristicsCreateServiceRequest(
            allergyIds,
            likeFoodIds,
            likeIngredientIds,
            otherCharacteristics
        );
    }
}
