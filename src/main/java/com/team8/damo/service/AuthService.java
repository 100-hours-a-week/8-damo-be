package com.team8.damo.service;

import com.team8.damo.entity.RefreshToken;
import com.team8.damo.entity.User;
import com.team8.damo.exception.CustomException;
import com.team8.damo.kakao.KakaoResponse;
import com.team8.damo.kakao.KakaoUtil;
import com.team8.damo.repository.RefreshTokenRepository;
import com.team8.damo.repository.UserRepository;
import com.team8.damo.security.jwt.JwtProvider;
import com.team8.damo.service.response.JwtTokenResponse;
import com.team8.damo.service.response.UserOAuthResponse;
import com.team8.damo.util.Snowflake;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.team8.damo.exception.errorcode.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final Snowflake snowflake;
    private final KakaoUtil kakaoUtil;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public UserOAuthResponse oauthLogin(String code) {
        KakaoResponse.OAuthToken oAuthToken = kakaoUtil.getAccessToken(code);
        KakaoResponse.UserInfo userInfo = kakaoUtil.getUserInfo(oAuthToken.getAccess_token());

        Boolean isNew = false;
        Long providerId = userInfo.getId();
        String kakaoEmail = userInfo.getKakao_account().getEmail();

        User user = userRepository.findByEmail(kakaoEmail)
            .orElseGet(() -> join(snowflake.nextId(), kakaoEmail, providerId, isNew));

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtProvider.createRefreshToken(user.getId(), user.getEmail());
        refreshTokenRepository.save(new RefreshToken(user.getEmail(), refreshToken));

        return new UserOAuthResponse(isNew, user.getId(), user.getOnboardingStep(), accessToken, refreshToken);
    }

    @Transactional
    public JwtTokenResponse reissue(String refreshToken) {
        if (refreshToken == null) {
            throw new CustomException(REFRESH_TOKEN_NOT_FOUND);
        }

        try {
            jwtProvider.validateToken(refreshToken);
        } catch (ExpiredJwtException e) {
            throw new CustomException(REFRESH_TOKEN_EXPIRED);
        }

        Long userId = jwtProvider.getUserId(refreshToken);
        String email = jwtProvider.getEmail(refreshToken);

        RefreshToken savedRefreshToken = refreshTokenRepository.findById(email)
            .orElseThrow(() -> new CustomException(REFRESH_TOKEN_NOT_FOUND));

        if (savedRefreshToken.isNotSameToken(refreshToken)) {
            throw new CustomException(REFRESH_MISMATCH);
        }

        String newAccessToken = jwtProvider.createAccessToken(userId, email);
        String newRefreshToken = jwtProvider.createRefreshToken(userId, email);

        refreshTokenRepository.save(new RefreshToken(email, newRefreshToken));
        return new JwtTokenResponse(newAccessToken, newRefreshToken);
    }

    @Transactional
    public JwtTokenResponse test() {
        String email = "user2@test.com";
        String accessToken = jwtProvider.createAccessToken(2L, email);
        String refreshToken = jwtProvider.createRefreshToken(2L, email);
        refreshTokenRepository.save(new RefreshToken(email, refreshToken));
        return new JwtTokenResponse(accessToken, refreshToken);
    }

    @Transactional
    public JwtTokenResponse test(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
        String accessToken = jwtProvider.createAccessToken(userId, user.getEmail());
        String refreshToken = jwtProvider.createRefreshToken(userId, user.getEmail());
        refreshTokenRepository.save(new RefreshToken(user.getEmail(), refreshToken));
        return new JwtTokenResponse(accessToken, refreshToken);
    }

    private User join(Long id, String email, Long providerId, Boolean isNew) {
        isNew = true;
        User user = new User(id, email, providerId);
        return userRepository.save(user);
    }
}
