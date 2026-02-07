package com.team8.damo.security.handler;

import com.team8.damo.entity.RefreshToken;
import com.team8.damo.exception.CustomException;
import com.team8.damo.exception.errorcode.ErrorCode;
import com.team8.damo.repository.RefreshTokenRepository;
import com.team8.damo.security.jwt.JwtUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomLogoutHandler implements LogoutHandler {
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public void logout(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) {
        if (authentication != null) {
            JwtUserDetails user = (JwtUserDetails) authentication.getPrincipal();
            if (user != null) {
                RefreshToken refreshToken = refreshTokenRepository.findById(user.getUsername())
                    .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));
                refreshTokenRepository.delete(refreshToken);
            }
        }
    }
}
