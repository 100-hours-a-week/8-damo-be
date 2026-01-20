package com.team8.damo.entity.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenType {
    ACCESS("엑세스 토큰"),
    REFRESH("리프레시 토큰");

    private final String description;
}
