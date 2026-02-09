package com.team8.damo.controller;

import com.team8.damo.controller.docs.UserControllerDocs;
import com.team8.damo.controller.request.UserBasicUpdateRequest;
import com.team8.damo.controller.request.UserCharacteristicsCreateRequest;
import com.team8.damo.controller.request.ImagePathUpdateRequest;
import com.team8.damo.controller.request.UserCharacteristicsUpdateRequest;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.service.response.UserBasicResponse;
import com.team8.damo.service.response.UserProfileResponse;
import com.team8.damo.security.jwt.JwtUserDetails;
import com.team8.damo.service.LightningService;
import com.team8.damo.service.UserService;
import com.team8.damo.service.response.LightningResponse;
import com.team8.damo.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;



@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController implements UserControllerDocs {

    private final UserService userService;
    private final LightningService lightningService;

    @GetMapping("/me/basic")
    public BaseResponse<UserBasicResponse> getBasic(
        @AuthenticationPrincipal JwtUserDetails user
    ) {
        return BaseResponse.ok(userService.getUserBasic(user.getUserId()));
    }

    @PatchMapping("/me/basic")
    public BaseResponse<Void> updateBasic(
        @AuthenticationPrincipal JwtUserDetails user,
        @Valid @RequestBody UserBasicUpdateRequest request
    ) {
        userService.updateUserBasic(user.getUserId(), request.toServiceRequest());
        return BaseResponse.noContent();
    }

    @PatchMapping("/me/image-path")
    public BaseResponse<Void> updateImagePath(
        @AuthenticationPrincipal JwtUserDetails user,
        @RequestBody ImagePathUpdateRequest request
    ) {
        userService.changeImagePath(user.getUserId(), request.getImagePath());
        return BaseResponse.noContent();
    }

    @PostMapping("/me/characteristics")
    public BaseResponse<Void> createCharacteristics(
        @AuthenticationPrincipal JwtUserDetails user,
        @Valid @RequestBody UserCharacteristicsCreateRequest request
    ) {
        userService.createCharacteristics(user.getUserId(), request.toServiceRequest());
        return BaseResponse.created(null);
    }

    @GetMapping("/me/profile")
    public BaseResponse<UserProfileResponse> getProfile(
        @AuthenticationPrincipal JwtUserDetails user
    ) {
        return BaseResponse.ok(userService.getUserProfile(user.getUserId()));
    }

    @PatchMapping("/me/characteristics")
    public BaseResponse<Void> updateProfile(
        @AuthenticationPrincipal JwtUserDetails user,
        @Valid @RequestBody UserCharacteristicsUpdateRequest request
    ) {
        userService.updateUserCharacteristics(user.getUserId(), request.toServiceRequest());
        return BaseResponse.noContent();
    }

    @GetMapping("/me/lightning")
    public BaseResponse<List<LightningResponse>> getParticipantLightningList(
        @AuthenticationPrincipal JwtUserDetails user
    ) {
        return BaseResponse.ok(lightningService.getParticipantLightningList(user.getUserId(), LocalDateTime.now(), 3));
    }

    @DeleteMapping("/me")
    public BaseResponse<Void> withdraw(
        @AuthenticationPrincipal JwtUserDetails user,
        HttpServletResponse response
    ) {
        userService.withdraw(user.getUserId());
        CookieUtil.deleteCookie(response, "access_token");
        CookieUtil.deleteCookie(response, "refresh_token");
        return BaseResponse.noContent();
    }
}
