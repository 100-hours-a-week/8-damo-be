package com.team8.damo.util;

import com.team8.damo.entity.enumeration.TokenType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import static com.team8.damo.entity.enumeration.TokenType.*;

@Component
public class CookieUtil {
    private static long accessTokenExpTime;
    private static long refreshTokenExpTime;

    public CookieUtil(
        @Value("${jwt.expiration.access}") long accessExp,
        @Value("${jwt.expiration.refresh}") long refreshExp
    ) {
        CookieUtil.accessTokenExpTime = accessExp;
        CookieUtil.refreshTokenExpTime = refreshExp;
    }

    public static void addCookie(HttpServletResponse response, TokenType tokenType, String token) {
        boolean isAccess = tokenType.equals(ACCESS);
        String cookieName = isAccess ? "access_token" : "refresh_token";
        ResponseCookie cookie = ResponseCookie.from(cookieName, token)
            .path("/")
            .sameSite(isAccess ? "Lax" : "Strict")
            .httpOnly(true)
            .secure(true)
            .maxAge(isAccess ? accessTokenExpTime : refreshTokenExpTime)
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
