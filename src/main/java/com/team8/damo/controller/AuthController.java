package com.team8.damo.controller;

import com.team8.damo.controller.docs.AuthControllerDocs;
import com.team8.damo.controller.request.OAuthLoginRequest;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.service.AuthService;
import com.team8.damo.service.response.JwtTokenResponse;
import com.team8.damo.service.response.OAuthLoginResponse;
import com.team8.damo.service.response.UserOAuthResponse;
import com.team8.damo.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static com.team8.damo.entity.enumeration.TokenType.ACCESS;
import static com.team8.damo.entity.enumeration.TokenType.REFRESH;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController implements AuthControllerDocs {
    private final AuthService authService;

    @Override
    @PostMapping("/oauth")
    public BaseResponse<OAuthLoginResponse> oauth(
        HttpServletResponse response,
        @Valid @RequestBody OAuthLoginRequest request
    ) {
        UserOAuthResponse oAuthResponse = authService.oauthLogin(request.getCode());
        CookieUtil.addCookie(response, ACCESS, oAuthResponse.getAccessToken());
        CookieUtil.addCookie(response, REFRESH, oAuthResponse.getRefreshToken());
        OAuthLoginResponse oAuthLoginResponse = new OAuthLoginResponse(oAuthResponse.getUserId(), oAuthResponse.getOnboardingStep());
        return BaseResponse.ok(oAuthLoginResponse);
    }

    @Override
    @PostMapping("/reissue")
    public BaseResponse<Void> reissue(
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        String refreshToken = CookieUtil.getRefreshToken(request);
        JwtTokenResponse jwtTokenResponse = authService.reissue(refreshToken);
        CookieUtil.addCookie(response, ACCESS, jwtTokenResponse.accessToken());
        CookieUtil.addCookie(response, REFRESH, jwtTokenResponse.refreshToken());
        return BaseResponse.noContent();
    }

    @Override
    @PostMapping("/test")
    public BaseResponse<Void> test(
        HttpServletResponse response
    ) {
        JwtTokenResponse jwtTokenResponse = authService.test();
        CookieUtil.addCookie(response, ACCESS, jwtTokenResponse.accessToken());
        CookieUtil.addCookie(response, REFRESH, jwtTokenResponse.refreshToken());
        return BaseResponse.noContent();
    }

    @Override
    @PostMapping("/test/{userId}")
    public BaseResponse<JwtTokenResponse> test(
        @PathVariable Long userId,
        HttpServletResponse response
    ) {
        JwtTokenResponse jwtTokenResponse = authService.test(userId);
        CookieUtil.addCookie(response, ACCESS, jwtTokenResponse.accessToken());
        CookieUtil.addCookie(response, REFRESH, jwtTokenResponse.refreshToken());
        return BaseResponse.ok(jwtTokenResponse);
    }
}
