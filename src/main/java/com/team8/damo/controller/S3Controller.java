package com.team8.damo.controller;

import com.team8.damo.controller.docs.S3ControllerDocs;
import com.team8.damo.controller.request.PresignedUrlRequest;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.security.jwt.JwtUserDetails;
import com.team8.damo.service.S3Service;
import com.team8.damo.service.response.PresignedUrlResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/s3")
public class S3Controller implements S3ControllerDocs {

    private final S3Service s3Service;

    @Override
    @PutMapping("/presigned-url")
    public BaseResponse<PresignedUrlResponse> getPresignedUrl(
        @AuthenticationPrincipal JwtUserDetails user,
        @Valid @RequestBody PresignedUrlRequest request
    ) {
        return BaseResponse.ok(s3Service.generatePresignedUrl(request.toServiceRequest()));
    }
}
