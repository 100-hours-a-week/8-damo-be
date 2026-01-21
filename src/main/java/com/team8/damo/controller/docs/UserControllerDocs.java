package com.team8.damo.controller.docs;

import com.team8.damo.controller.request.UserBasicUpdateRequest;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.security.jwt.JwtUserDetails;
import com.team8.damo.swagger.annotation.ApiErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import static com.team8.damo.exception.errorcode.ErrorCode.DUPLICATE_NICKNAME;
import static com.team8.damo.exception.errorcode.ErrorCode.USER_NOT_FOUND;

@Tag(name = "User API", description = "사용자 관련 API")
public interface UserControllerDocs {

    @Operation(
        summary = "사용자 기본 정보 수집",
        description = """
            ### 사용자 기본 정보를 수집합니다.
            - 닉네임: 1~10자, 공백/특수문자 불가, 영문/숫자/한글만 허용
            - 성별: MALE, FEMALE
            - 연령대: TWENTIES, THIRTIES, FORTIES, FIFTIES_PLUS
            """
    )
    @ApiResponse(responseCode = "204", description = "성공")
    @ApiErrorResponses({USER_NOT_FOUND, DUPLICATE_NICKNAME})
    BaseResponse<Void> updateBasic(
        @Parameter(hidden = true)
        JwtUserDetails user,
        UserBasicUpdateRequest request
    );
}
