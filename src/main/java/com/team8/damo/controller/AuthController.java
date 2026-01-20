package com.team8.damo.controller;

import com.team8.damo.controller.request.OAuthLoginRequest;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.controller.response.OAuthLoginResponse;
import com.team8.damo.controller.response.UserOAuthResponse;
import com.team8.damo.service.AuthService;
import com.team8.damo.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.team8.damo.entity.enumeration.TokenType.ACCESS;
import static com.team8.damo.entity.enumeration.TokenType.REFRESH;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/oauth")
    public BaseResponse<?> oauth(
        HttpServletResponse response,
        @Valid @RequestBody OAuthLoginRequest request
    ) {
        UserOAuthResponse oAuthResponse = authService.oauthLogin(request.getCode());
        CookieUtil.addCookie(response, ACCESS, oAuthResponse.getAccessToken());
        CookieUtil.addCookie(response, REFRESH, oAuthResponse.getRefreshToken());
        OAuthLoginResponse oAuthLoginResponse = new OAuthLoginResponse(oAuthResponse.getUserId(), oAuthResponse.getOnboardingStep());
        return BaseResponse.ok(oAuthLoginResponse);
    }
}
