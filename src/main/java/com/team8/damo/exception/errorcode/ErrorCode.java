package com.team8.damo.exception.errorcode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.HttpStatus.CONFLICT;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    USER_NOT_FOUND(NOT_FOUND, "사용자를 찾을 수 없습니다."),
    DUPLICATE_NICKNAME(CONFLICT, "이미 사용중인 닉네임입니다."),
    INVALID_CATEGORY(BAD_REQUEST, "잘못된 카테고리입니다."),
    DUPLICATE_ALLERGY_CATEGORY(CONFLICT, "알레르기 카테고리가 중복 선택 되었습니다."),
    DUPLICATE_LIKE_FOOD_CATEGORY(CONFLICT, "선호 음식 카테고리가 중복 선택 되었습니다."),
    DUPLICATE_LIKE_INGREDIENT_CATEGORY(CONFLICT, "선호 재료 카테고리가 중복 선택 되었습니다."),

    JSON_PARSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "JSON 파싱 오류입니다."),
    NOT_FOUND_END_POINT(HttpStatus.NOT_FOUND, "존재하지 않는 API 입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "예상치 못한 서버 오류입니다."),

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
}
