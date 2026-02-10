package com.team8.damo.controller.docs;

import com.team8.damo.controller.request.LightningCreateRequest;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.security.jwt.JwtUserDetails;
import com.team8.damo.swagger.annotation.ApiErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import static com.team8.damo.exception.errorcode.ErrorCode.*;

@Tag(name = "Lightning Gathering API", description = "번개 모임 관련 API")
public interface LightningControllerDocs {

    @Operation(
        summary = "번개 모임 참가",
        description = "번개 모임에 참가합니다."
    )
    @ApiResponse(responseCode = "201", description = "성공")
    @ApiErrorResponses({LIGHTNING_NOT_FOUND, LIGHTNING_CLOSED, DUPLICATE_LIGHTNING_PARTICIPANT, LIGHTNING_CAPACITY_EXCEEDED, USER_NOT_FOUND})
    BaseResponse<Long> joinLightning(
        @Parameter(hidden = true)
        JwtUserDetails user,
        @Parameter(description = "번개 모임 ID", required = true)
        Long lightningId
    );

    @Operation(
        summary = "번개 모임 모집 마감",
        description = "번개 모임의 모집을 마감합니다. 모임장(LEADER)만 가능합니다."
    )
    @ApiResponse(responseCode = "204", description = "성공")
    @ApiErrorResponses({LIGHTNING_PARTICIPANT_NOT_FOUND, LIGHTNING_CLOSE_ONLY_LEADER, LIGHTNING_ALREADY_CLOSED})
    BaseResponse<Void> closeLightning(
        @Parameter(hidden = true)
        JwtUserDetails user,
        @Parameter(description = "번개 모임 ID", required = true)
        Long lightningId
    );

    @Operation(
        summary = "번개 모임 나가기",
        description = "번개 모임에서 나갑니다. PARTICIPANT는 바로 나갈 수 있고, LEADER는 혼자일 때만 나갈 수 있습니다."
    )
    @ApiResponse(responseCode = "204", description = "성공")
    @ApiErrorResponses({LIGHTNING_PARTICIPANT_NOT_FOUND, LIGHTNING_LEADER_CANNOT_LEAVE})
    BaseResponse<Void> leaveLightning(
        @Parameter(hidden = true)
        JwtUserDetails user,
        @Parameter(description = "번개 모임 ID", required = true)
        Long lightningId
    );

    @Operation(
        summary = "번개 모임 생성",
        description = """
            ### 새로운 번개 모임을 생성합니다.
            - restaurantId: 식당 ID (필수)
            - maxParticipants: 최대 참여 인원 (2~8명)
            - description: 설명 (최대 30자, 선택)
            - 생성자는 자동으로 리더(LEADER)가 됩니다.
            """
    )
    @ApiResponse(responseCode = "201", description = "성공")
    @ApiErrorResponses({USER_NOT_FOUND, LIGHTNING_DATE_MUST_BE_AFTER_NOW})
    BaseResponse<Long> createLightning(
        @Parameter(hidden = true)
        JwtUserDetails user,
        LightningCreateRequest request
    );
}
