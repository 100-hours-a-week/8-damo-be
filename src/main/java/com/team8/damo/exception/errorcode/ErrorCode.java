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

    // Group
    GROUP_NOT_FOUND(NOT_FOUND, "그룹을 찾을 수 없습니다."),
    DUPLICATE_GROUP_MEMBER(CONFLICT, "이미 참여중인 그룹입니다."),
    USER_NOT_GROUP_MEMBER(FORBIDDEN, "그룹에 속하지 않아 접근할 수 없습니다."),

    // Dining
    ONLY_GROUP_LEADER_ALLOWED(FORBIDDEN, "회식 생성은 그룹장만 가능합니다."),
    DINING_DATE_MUST_BE_AFTER_NOW(BAD_REQUEST, "회식 진행 날짜는 현재 날짜보다 이후여야 합니다."),
    VOTE_DUE_DATE_MUST_BE_BEFORE_DINING_DATE(BAD_REQUEST, "투표 마감 날짜는 회식 진행 날짜 이전이어야 합니다."),
    DINING_LIMIT_EXCEEDED(BAD_REQUEST, "회식 완료가 되지 않은 회식이 3개 이상이므로 회식을 생성할 수 없습니다."),
    DINING_NOT_FOUND(NOT_FOUND, "회식을 찾을 수 없습니다."),
    INVALID_VOTE_STATUS(BAD_REQUEST, "유효하지 않은 투표 상태입니다."),
    ATTENDANCE_VOTING_CLOSED(BAD_REQUEST, "참석/불참석 투표가 종료되었습니다."),
    NO_VOTE_PERMISSION(FORBIDDEN, "참석, 불참석 투표 권한이 존재하지 않습니다."),
    ATTENDANCE_VOTE_ALREADY_COMPLETED(CONFLICT, "참석, 불참석 투표를 이미 완료했습니다."),

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
