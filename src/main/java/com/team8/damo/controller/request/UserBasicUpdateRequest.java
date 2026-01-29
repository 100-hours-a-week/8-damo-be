package com.team8.damo.controller.request;

import com.team8.damo.entity.enumeration.AgeGroup;
import com.team8.damo.entity.enumeration.Gender;
import com.team8.damo.service.request.UserBasicUpdateServiceRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UserBasicUpdateRequest {

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 1, max = 10, message = "닉네임은 1자 이상, 10자 이하까지 가능합니다.")
    @Pattern(regexp = "^[a-zA-Z0-9가-힣]+$", message = "닉네임에 공백이나 특수문자를 포함할 수 없습니다.")
    private String nickname;

    @NotNull(message = "성별은 필수입니다.")
    private Gender gender;

    @NotNull(message = "연령대는 필수입니다.")
    private AgeGroup ageGroup;

    @NotNull
    private String imagePath;

    public UserBasicUpdateServiceRequest toServiceRequest() {
        return new UserBasicUpdateServiceRequest(nickname, gender, ageGroup, imagePath);
    }
}
