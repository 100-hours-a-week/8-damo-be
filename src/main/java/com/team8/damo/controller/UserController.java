package com.team8.damo.controller;

import com.team8.damo.controller.request.UserBasicUpdateRequest;
import com.team8.damo.security.jwt.JwtUserDetails;
import com.team8.damo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @PatchMapping("/me/basic")
    public ResponseEntity<?> updateBasic(
        @AuthenticationPrincipal JwtUserDetails user,
        @Valid @RequestBody UserBasicUpdateRequest request
    ) {
        userService.updateUserBasic(user.getUserId(), request.toServiceRequest());
        return ResponseEntity.noContent().build();
    }
}
