package com.team8.damo.controller.request;

import com.team8.damo.service.request.GroupCreateServiceRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class GroupCreateRequest {

    @NotBlank(message = "그룹명은 필수입니다.")
    @Size(min = 2, max = 10, message = "그룹명은 2자 이상, 10자 이하까지 가능합니다.")
    private String name;

    @Size(max = 30, message = "소개글은 최대 30자까지 가능합니다.")
    private String introduction;

    private double latitude;

    private double longitude;

    @NotNull
    private String imagePath;

    public GroupCreateServiceRequest toServiceRequest() {
        return new GroupCreateServiceRequest(name, introduction, latitude, longitude, imagePath);
    }
}
