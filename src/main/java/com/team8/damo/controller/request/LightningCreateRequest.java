package com.team8.damo.controller.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.team8.damo.service.request.LightningCreateServiceRequest;
import jakarta.validation.constraints.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class LightningCreateRequest {

    @NotBlank(message = "식당 ID는 필수입니다.")
    private String restaurantId;

    @Min(value = 2, message = "참여 인원은 최소 2명 이상이어야 합니다.")
    @Max(value = 8, message = "참여 인원은 최대 8명 이하여야 합니다.")
    private int maxParticipants;

    @Size(max = 30, message = "설명은 최대 30자까지 가능합니다.")
    private String description;

    @NotNull(message = "번개 진행 날짜는 필수입니다.")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime lightningDate;

    public LightningCreateServiceRequest toServiceRequest() {
        return new LightningCreateServiceRequest(restaurantId, maxParticipants, description, lightningDate);
    }
}
