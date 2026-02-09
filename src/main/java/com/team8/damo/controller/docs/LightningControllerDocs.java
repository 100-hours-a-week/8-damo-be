package com.team8.damo.controller.docs;

import com.team8.damo.controller.request.LightningCreateRequest;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.security.jwt.JwtUserDetails;
import com.team8.damo.swagger.annotation.ApiErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;

import static com.team8.damo.exception.errorcode.ErrorCode.LIGHTNING_DATE_MUST_BE_AFTER_NOW;
import static com.team8.damo.exception.errorcode.ErrorCode.USER_NOT_FOUND;

@Tag(name = "Lightning Gathering API", description = "번개 모임 관련 API")
public interface LightningControllerDocs {

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
