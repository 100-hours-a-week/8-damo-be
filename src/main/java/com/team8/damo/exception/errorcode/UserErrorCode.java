package com.team8.damo.exception.errorcode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(NOT_FOUND, "사용자를 찾을 수 없습니다."),
    DUPLICATE_NICKNAME(CONFLICT, "이미 사용중인 닉네임입니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
