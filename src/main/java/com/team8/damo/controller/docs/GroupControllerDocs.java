package com.team8.damo.controller.docs;

import com.team8.damo.controller.request.GroupCreateRequest;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.security.jwt.JwtUserDetails;
import com.team8.damo.swagger.annotation.ApiErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import static com.team8.damo.exception.errorcode.ErrorCode.*;

@Tag(name = "Group API", description = "그룹 관련 API")
public interface GroupControllerDocs {

    @Operation(
        summary = "그룹 생성",
        description = """
            ### 새로운 그룹을 생성합니다.
            - name: 그룹명 (2~10자)
            - introduction: 소개글 (최대 30자, 선택)
            - latitude: 위도
            - longitude: 경도
            - 생성자는 자동으로 그룹장(LEADER)이 됩니다.
            """
    )
    @ApiResponse(responseCode = "201", description = "성공")
    @ApiErrorResponses({USER_NOT_FOUND})
    BaseResponse<Long> createGroup(
        @Parameter(hidden = true)
        JwtUserDetails user,
        GroupCreateRequest request
    );
}
