package com.team8.damo.controller;

import com.team8.damo.controller.docs.UserControllerDocs;
import com.team8.damo.controller.request.UserBasicUpdateRequest;
import com.team8.damo.controller.request.UserCharacteristicsCreateRequest;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.service.response.UserBasicResponse;
import com.team8.damo.service.response.UserProfileResponse;
import com.team8.damo.security.jwt.JwtUserDetails;
import com.team8.damo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;



@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController implements UserControllerDocs {

    private final UserService userService;

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
}
