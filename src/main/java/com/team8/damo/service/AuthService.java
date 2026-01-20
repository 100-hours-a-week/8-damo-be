package com.team8.damo.service;

import com.team8.damo.service.response.UserOAuthResponse;
import com.team8.damo.entity.User;
import com.team8.damo.kakao.KakaoResponse;
import com.team8.damo.kakao.KakaoUtil;
import com.team8.damo.repository.UserRepository;
import com.team8.damo.security.jwt.JwtProvider;
import com.team8.damo.util.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final Snowflake snowflake;
    private final KakaoUtil kakaoUtil;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

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
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        return new UserOAuthResponse(isNew, user.getId(), user.getOnboardingStep(), accessToken, refreshToken);
    }

    private User join(Long id, String email, Long providerId, Boolean isNew) {
        isNew = true;
        User user = new User(id, email, providerId);
        return userRepository.save(user);
    }
}
