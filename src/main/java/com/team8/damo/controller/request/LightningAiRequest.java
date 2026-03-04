package com.team8.damo.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LightningAiRequest {

    @NotBlank(message = "경도(x)는 필수입니다.")
    private String x;

    @NotBlank(message = "위도(y)는 필수입니다.")
    private String y;

}
