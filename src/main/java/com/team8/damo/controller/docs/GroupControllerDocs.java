package com.team8.damo.controller.docs;

import com.team8.damo.controller.request.GroupCreateRequest;
import com.team8.damo.controller.request.ImagePathUpdateRequest;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.service.response.GroupDetailResponse;
import com.team8.damo.security.jwt.JwtUserDetails;
import com.team8.damo.swagger.annotation.ApiErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.team8.damo.service.response.UserGroupResponse;

import java.util.List;

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

    @Operation(
        summary = "그룹 프로필 이미지 경로 수정",
        description = """
            ### 그룹 프로필 이미지 경로를 수정합니다.
            - imagePath: S3 업로드 후 반환된 objectKey
            """
    )
    @ApiResponse(responseCode = "204", description = "성공")
    @ApiErrorResponses({GROUP_NOT_FOUND})
    BaseResponse<Void> updateImagePath(
        @Parameter(hidden = true)
        JwtUserDetails user,
        @Parameter(description = "그룹 ID", required = true)
        Long groupId,
        ImagePathUpdateRequest request
    );

    @Operation(
        summary = "내 그룹 목록 조회",
        description = """
            ### 사용자가 속한 그룹 목록을 조회합니다.
            - groupId: 그룹 ID
            - name: 그룹명
            - introduction: 소개글
            """
    )
    @ApiResponse(
        responseCode = "200",
        description = "성공",
        content = @Content(
            mediaType = "application/json",
            array = @ArraySchema(schema = @Schema(implementation = UserGroupResponse.class))
        )
    )
    BaseResponse<List<UserGroupResponse>> groupList(
        @Parameter(hidden = true)
        JwtUserDetails user
    );

    @Operation(
        summary = "그룹 상세 조회",
        description = """
            ### 그룹 상세 정보를 조회합니다.
            - name: 그룹명
            - introduction: 소개글
            - participantsCount: 참여자 수
            - isGroupLeader: 그룹장 여부

            **접근 권한**: 해당 그룹에 속한 사용자만 조회 가능
            """
    )
    @ApiResponse(
        responseCode = "200",
        description = "성공",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = GroupDetailResponse.class)
        )
    )
    @ApiErrorResponses({USER_NOT_GROUP_MEMBER})
    BaseResponse<GroupDetailResponse> getGroupDetail(
        @Parameter(hidden = true)
        JwtUserDetails user,
        @Parameter(description = "그룹 ID", required = true)
        Long groupId
    );

    @Operation(
        summary = "그룹 참여",
        description = """
            ### 그룹에 참여합니다.
            - 사용자가 해당 그룹의 일반 참여자(PARTICIPANT)로 등록됩니다.
            - 이미 참여중인 그룹에는 중복 참여할 수 없습니다.
            """
    )
    @ApiResponse(responseCode = "201", description = "성공")
    @ApiErrorResponses({USER_NOT_FOUND, GROUP_NOT_FOUND, DUPLICATE_GROUP_MEMBER})
    BaseResponse<Long> attend(
        @Parameter(hidden = true)
        JwtUserDetails user,
        @Parameter(description = "그룹 ID", required = true)
        Long groupId
    );
}
