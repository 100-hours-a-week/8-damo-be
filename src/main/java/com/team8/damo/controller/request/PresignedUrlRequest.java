package com.team8.damo.controller.request;

import com.team8.damo.service.request.PresignedUrlServiceRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PresignedUrlRequest {

    @NotBlank(message = "파일명은 필수입니다.")
    private String fileName;

    @NotBlank(message = "Content-Type은 필수입니다.")
    private String contentType;

    private String directory;

    public PresignedUrlServiceRequest toServiceRequest() {
        return new PresignedUrlServiceRequest(fileName, contentType, directory);
    }
}
