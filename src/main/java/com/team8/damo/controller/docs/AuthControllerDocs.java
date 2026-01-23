package com.team8.damo.controller.docs;

import com.team8.damo.controller.request.OAuthLoginRequest;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.service.response.OAuthLoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

@Tag(name = "Auth API", description = "인증 관련 API")
public interface AuthControllerDocs {

    @Operation(
        summary = "카카오 OAuth 로그인",
        description = """
            ### 카카오 OAuth 인가 코드를 통해 로그인합니다.
            - code: 카카오 인가 코드

            **응답**:
            - userId: 사용자 ID
            - onboardingStep: 온보딩 단계 (BASIC, CHARACTERISTIC, DONE)

            **쿠키 설정**:
            - access_token: 액세스 토큰 (HttpOnly, Secure, SameSite=Lax)
            - refresh_token: 리프레시 토큰 (HttpOnly, Secure, SameSite=Strict)
            """
    )
    @ApiResponse(responseCode = "200", description = "성공")
    BaseResponse<OAuthLoginResponse> oauth(
        @Parameter(hidden = true)
        HttpServletResponse response,
        OAuthLoginRequest request
    );
}
