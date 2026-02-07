package com.team8.damo.security.jwt;

import com.team8.damo.exception.CustomException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import static com.team8.damo.exception.errorcode.ErrorCode.*;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        String accessToken = request.getHeader("Authorization");

        try {
            if (accessToken != null && accessToken.startsWith("Bearer ")) {
                String token = accessToken.substring(7);

                if (jwtProvider.validateToken(token)) {
                    Authentication authentication = jwtProvider.getAuthentication(token);
                    if (authentication != null) {
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }

            filterChain.doFilter(request, response); // 다음 필터로 넘기기
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            throw new CustomException(JWT_INVALID_TOKEN_ERROR);
        } catch (ExpiredJwtException e) {
            throw new CustomException(JWT_EXPIRED_TOKEN_ERROR);
        } catch (UnsupportedJwtException e) {
            throw new CustomException(JWT_UNSUPPORTED_TOKEN_ERROR);
        } catch (IllegalArgumentException e) {
            throw new CustomException(JWT_CLAIMS_EMPTY_ERROR);
        } catch (Exception e) {
            throw new CustomException(JWT_FILTER_ERROR);
        }
    }
}
