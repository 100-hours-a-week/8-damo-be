package com.team8.damo.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class OAuthLoginRequest {

    @NotBlank(message = "인가 코드는 비어있을 수 없습니다.")
    private String code;
}
