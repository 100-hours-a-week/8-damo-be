package com.team8.damo.controller.docs;

import com.team8.damo.controller.request.ChatMessagePageRequest;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.security.jwt.JwtUserDetails;
import com.team8.damo.service.response.ChatMessagePageResponse;
import com.team8.damo.swagger.annotation.ApiErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import static com.team8.damo.exception.errorcode.ErrorCode.LIGHTNING_PARTICIPANT_NOT_FOUND;

@Tag(name = "Chat API", description = "채팅 관련 API")
public interface ChatMessageControllerDocs {

    @Operation(
        summary = "채팅 메시지 커서 기반 페이지네이션 조회",
        description = """
            ### 채팅 메시지를 커서 기반으로 페이지네이션하여 조회합니다.
            #### 요청 파라미터
            - direction: PREV, NEXT (cursorId가 있을 때 사용, null이면 NEXT로 처리)
            - cursorId: 기준 메시지 ID
            - size: 페이지 크기 (1~100)

            #### 조회 동작
            - **INIT 모드** (cursorId 미전달):
              - 첫 입장(anchor=0): TOP(가장 오래된 메시지부터)
              - 미읽음 없음(latest<=anchor): BOTTOM(가장 최신 메시지 구간)
              - 미읽음 있음(latest>anchor): CENTER(anchor 기준 전후 메시지 병합)
            - **PREV 방향**: cursorId보다 이전 메시지 조회
            - **NEXT 방향**: cursorId보다 이후 메시지 조회

            #### 응답 필드
            - pageInfo:
              - prevCursor, nextCursor
              - hasPreviousPage, hasNextPage
              - previousPageParam, nextPageParam
            - initialScrollMode: TOP, CENTER, BOTTOM, NONE
            - readBoundary:
              - showDivider: \"여기까지 읽었습니다.\" 표시 여부
              - lastReadMessageId, firstUnreadMessageId

            #### 읽음 커서 처리
            - 채팅방 입장 시(cursorId 미전달) 해당 번개의 최신 메시지 ID로 `lastReadChatMessageId`를 초기화합니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiErrorResponses({LIGHTNING_PARTICIPANT_NOT_FOUND})
    BaseResponse<ChatMessagePageResponse> getChatMessages(
        @Parameter(hidden = true)
        JwtUserDetails user,
        @Parameter(description = "번개 모임 ID", required = true)
        Long lightningId,
        ChatMessagePageRequest request
    );
}
