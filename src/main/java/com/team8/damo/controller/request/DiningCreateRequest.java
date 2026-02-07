package com.team8.damo.controller.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.team8.damo.service.request.DiningCreateServiceRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class DiningCreateRequest {

    @NotNull(message = "회식 날짜는 필수입니다.")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime diningDate;

    @NotNull(message = "투표 마감 날짜는 필수입니다.")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime voteDueDate;

    @NotNull(message = "예산은 필수입니다.")
    @Min(value = 0, message = "예산은 0 이상이어야 합니다.")
    private Integer budget;

    public DiningCreateServiceRequest toServiceRequest() {
        return new DiningCreateServiceRequest(diningDate, voteDueDate, budget);
    }
}
