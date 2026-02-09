package com.team8.damo.controller;

import com.team8.damo.controller.docs.LightningControllerDocs;
import com.team8.damo.controller.request.LightningCreateRequest;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.security.jwt.JwtUserDetails;
import com.team8.damo.service.LightningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class LightningController implements LightningControllerDocs {

    private final LightningService lightningService;

    @Override
    @PostMapping("/lightning")
    public BaseResponse<Long> createLightning(
        @AuthenticationPrincipal JwtUserDetails user,
        @Valid @RequestBody LightningCreateRequest request
    ) {
        return BaseResponse.created(
            lightningService.createLightning(user.getUserId(), request.toServiceRequest(), LocalDateTime.now())
        );
    }
}
