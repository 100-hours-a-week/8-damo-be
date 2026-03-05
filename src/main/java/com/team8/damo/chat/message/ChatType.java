package com.team8.damo.chat.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatType {
    TEXT("채팅"),
    JOIN("참가"),
    LEAVE("퇴장");

    private final String description;
}
