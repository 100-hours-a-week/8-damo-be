package com.team8.damo.service;

import com.team8.damo.entity.RefreshToken;
import com.team8.damo.exception.CustomException;
import com.team8.damo.kakao.KakaoUtil;
import com.team8.damo.repository.RefreshTokenRepository;
import com.team8.damo.repository.UserRepository;
import com.team8.damo.security.jwt.JwtProvider;
import com.team8.damo.service.response.JwtTokenResponse;
import com.team8.damo.util.Snowflake;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static com.team8.damo.exception.errorcode.ErrorCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private Snowflake snowflake;

    @Mock
    private KakaoUtil kakaoUtil;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("reissue 메서드는")
    class ReissueTest {

        @Test
        @DisplayName("유효한 리프레시 토큰으로 새로운 토큰을 재발급한다.")
        void reissue_success() {
            // given
            String refreshToken = "valid-refresh-token";
            String email = "test@test.com";
            Long userId = 1L;
            String newAccessToken = "new-access-token";
            String newRefreshToken = "new-refresh-token";

            given(jwtProvider.validateToken(refreshToken)).willReturn(true);
            given(jwtProvider.getUserId(refreshToken)).willReturn(userId);
            given(jwtProvider.getEmail(refreshToken)).willReturn(email);
            given(refreshTokenRepository.findById(email))
                .willReturn(Optional.of(new RefreshToken(email, refreshToken)));
            given(jwtProvider.createAccessToken(userId, email)).willReturn(newAccessToken);
            given(jwtProvider.createRefreshToken(userId, email)).willReturn(newRefreshToken);

            // when
            JwtTokenResponse result = authService.reissue(refreshToken);

            // then
            assertThat(result.accessToken()).isEqualTo(newAccessToken);
            assertThat(result.refreshToken()).isEqualTo(newRefreshToken);

            then(jwtProvider).should().validateToken(refreshToken);
            then(jwtProvider).should().getUserId(refreshToken);
            then(jwtProvider).should().getEmail(refreshToken);
            then(refreshTokenRepository).should().findById(email);
            then(jwtProvider).should().createAccessToken(userId, email);
            then(jwtProvider).should().createRefreshToken(userId, email);
            then(refreshTokenRepository).should().save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("만료된 리프레시 토큰으로 재발급 시 예외가 발생한다.")
        void reissue_fail_expiredToken() {
            // given
            String expiredToken = "expired-refresh-token";

            willThrow(new ExpiredJwtException(null, null, "Token expired"))
                .given(jwtProvider).validateToken(expiredToken);

            // when // then
            assertThatThrownBy(() -> authService.reissue(expiredToken))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", REFRESH_TOKEN_EXPIRED);

            then(jwtProvider).should().validateToken(expiredToken);
            then(jwtProvider).should(never()).getUserId(expiredToken);
            then(jwtProvider).should(never()).getEmail(expiredToken);
            then(refreshTokenRepository).should(never()).findById(expiredToken);
        }

        @Test
        @DisplayName("저장소에 존재하지 않는 리프레시 토큰으로 재발급 시 예외가 발생한다.")
        void reissue_fail_tokenNotFound() {
            // given
            String refreshToken = "unknown-refresh-token";
            String email = "unknown@test.com";
            Long userId = 1L;

            given(jwtProvider.validateToken(refreshToken)).willReturn(true);
            given(jwtProvider.getUserId(refreshToken)).willReturn(userId);
            given(jwtProvider.getEmail(refreshToken)).willReturn(email);
            given(refreshTokenRepository.findById(email)).willReturn(Optional.empty());

            // when // then
            assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", REFRESH_TOKEN_NOT_FOUND);

            then(jwtProvider).should().validateToken(refreshToken);
            then(jwtProvider).should().getUserId(refreshToken);
            then(jwtProvider).should().getEmail(refreshToken);
            then(refreshTokenRepository).should().findById(email);
            then(jwtProvider).should(never()).createAccessToken(userId, email);
        }

        @Test
        @DisplayName("저장된 토큰과 요청 토큰이 일치하지 않으면 예외가 발생한다.")
        void reissue_fail_tokenMismatch() {
            // given
            String refreshToken = "request-refresh-token";
            String savedToken = "saved-different-token";
            String email = "test@test.com";
            Long userId = 1L;

            given(jwtProvider.validateToken(refreshToken)).willReturn(true);
            given(jwtProvider.getUserId(refreshToken)).willReturn(userId);
            given(jwtProvider.getEmail(refreshToken)).willReturn(email);
            given(refreshTokenRepository.findById(email))
                .willReturn(Optional.of(new RefreshToken(email, savedToken)));

            // when // then
            assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", REFRESH_MISMATCH);

            then(jwtProvider).should().validateToken(refreshToken);
            then(jwtProvider).should().getUserId(refreshToken);
            then(jwtProvider).should().getEmail(refreshToken);
            then(refreshTokenRepository).should().findById(email);
            then(jwtProvider).should(never()).createAccessToken(userId, email);
        }
    }
}
