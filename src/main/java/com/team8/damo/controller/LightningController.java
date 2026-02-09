package com.team8.damo.controller;

import com.team8.damo.controller.docs.LightningControllerDocs;
import com.team8.damo.controller.request.LightningCreateRequest;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.security.jwt.JwtUserDetails;
import com.team8.damo.service.LightningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class LightningController implements LightningControllerDocs {

    private final LightningService lightningService;

    @Override
    @PostMapping("/lightning/{lightningId}/users/me")
    public BaseResponse<Long> joinLightning(
        @AuthenticationPrincipal JwtUserDetails user,
        @PathVariable Long lightningId
    ) {
        return BaseResponse.created(
            lightningService.joinLightning(user.getUserId(), lightningId)
        );
    }

    @Override
    @PatchMapping("/lightning/{lightningId}/close")
    public BaseResponse<Void> closeLightning(
        @AuthenticationPrincipal JwtUserDetails user,
        @PathVariable Long lightningId
    ) {
        lightningService.closeLightning(user.getUserId(), lightningId);
        return BaseResponse.noContent();
    }

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
