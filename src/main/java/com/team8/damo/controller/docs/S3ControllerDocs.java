package com.team8.damo.controller.docs;

import com.team8.damo.controller.request.PresignedUrlRequest;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.security.jwt.JwtUserDetails;
import com.team8.damo.service.response.PresignedUrlResponse;
import com.team8.damo.swagger.annotation.ApiErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import static com.team8.damo.exception.errorcode.ErrorCode.INVALID_FILE_TYPE;

@Tag(name = "S3 API", description = "S3 파일 업로드 관련 API")
public interface S3ControllerDocs {

    @Operation(
        summary = "Presigned URL 발급",
        description = """
            ### S3 업로드용 Presigned URL을 발급합니다.
            - fileName: 업로드할 파일명 (userId or groupId)
            - contentType: 파일의 MIME 타입 (허용: image/png, image/jpeg, image/jpg, image/webp)
            - directory: S3 내 저장 경로 (ex. groups/profile or users/profile)

            **사용 방법**:
            1. 이 API로 presigned URL 발급
            2. 발급받은 URL로 PUT 요청하여 파일 업로드

            **유효시간**: 5분
            """
    )
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiErrorResponses({INVALID_FILE_TYPE})
    BaseResponse<PresignedUrlResponse> getPresignedUrl(
        @Parameter(hidden = true)
        JwtUserDetails user,
        PresignedUrlRequest request
    );
}
