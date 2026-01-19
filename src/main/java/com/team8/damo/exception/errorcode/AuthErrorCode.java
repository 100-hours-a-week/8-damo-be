package com.team8.damo.exception.errorcode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    ACCESS_TOKEN_EXPIRED(UNAUTHORIZED, "엑세스 토큰이 만료되었습니다."),
    REFRESH_TOKEN_EXPIRED(UNAUTHORIZED, "리프레시 토큰이 만료되었습니다."),
    // JWT Filter Error
    JWT_FILTER_ERROR(FORBIDDEN, "JWT filter 인증 오류 접근 권한이 없거나 토큰이 존재 하지 않습니다."),
    JWT_INVALID_TOKEN_ERROR(UNAUTHORIZED, "유효하지 않은 JWT 입니다."),
    JWT_EXPIRED_TOKEN_ERROR(UNAUTHORIZED, "JWT 유효시간 만료입니다."),
    JWT_UNSUPPORTED_TOKEN_ERROR(UNAUTHORIZED, "지원하지 않는 JWT 형식입니다."),
    JWT_CLAIMS_EMPTY_ERROR(UNAUTHORIZED, "JWT Claim 접근 오류입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public HttpStatus getHttpStatus() {
        return null;
    }

    @Override
    public String getMessage() {
        return "";
    }
}
