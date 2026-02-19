package com.team8.damo.controller.request;

import com.team8.damo.entity.enumeration.SatisfactionType;
import com.team8.damo.service.request.ReviewCreateServiceRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
public class ReviewCreateRequest {

    @NotNull(message = "별점은 필수입니다.")
    @Min(value = 1, message = "별점은 1 이상이어야 합니다.")
    @Max(value = 5, message = "별점은 5 이하여야 합니다.")
    private Integer starRating;

    @NotEmpty(message = "만족 태그는 최소 1개 이상 선택해야 합니다.")
    private List<SatisfactionType> satisfactionTags;

    @Size(max = 200, message = "텍스트 후기는 200자 이내여야 합니다.")
    private String content;

    public ReviewCreateServiceRequest toServiceRequest() {
        return new ReviewCreateServiceRequest(starRating, satisfactionTags, content);
    }
}
