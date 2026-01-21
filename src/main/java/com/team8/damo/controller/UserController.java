package com.team8.damo.controller;

import com.team8.damo.controller.docs.UserControllerDocs;
import com.team8.damo.controller.request.UserBasicUpdateRequest;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.security.jwt.JwtUserDetails;
import com.team8.damo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController implements UserControllerDocs {

    private final UserService userService;

    @PatchMapping("/me/basic")
    public BaseResponse<Void> updateBasic(
        @AuthenticationPrincipal JwtUserDetails user,
        @Valid @RequestBody UserBasicUpdateRequest request
    ) {
        userService.updateUserBasic(user.getUserId(), request.toServiceRequest());
        return BaseResponse.noContent();
    }
}
