package com.team8.damo.controller.docs;

import com.team8.damo.controller.request.AttendanceVoteRequest;
import com.team8.damo.controller.request.DiningCreateRequest;
import com.team8.damo.controller.request.RestaurantVoteRequest;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.entity.enumeration.DiningStatus;
import com.team8.damo.entity.enumeration.VotingStatus;
import com.team8.damo.security.jwt.JwtUserDetails;
import com.team8.damo.service.response.DiningResponse;
import com.team8.damo.service.response.RestaurantVoteResponse;
import com.team8.damo.swagger.annotation.ApiErrorResponses;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import static com.team8.damo.exception.errorcode.ErrorCode.*;

@Tag(name = "Dining API", description = "회식 관련 API")
public interface DiningControllerDocs {

    @Operation(
        summary = "회식 생성",
        description = """
            ### 새로운 회식을 생성합니다.
            - diningDate: 회식 진행 날짜 (yyyy-MM-dd HH:mm)
            - voteDueDate: 투표 마감 날짜 (yyyy-MM-dd HH:mm)
            - budget: 예산 (0 이상)

            **생성 조건**:
            - 그룹장만 회식을 생성할 수 있습니다.
            - 회식 진행 날짜는 현재 날짜보다 이후여야 합니다.
            - 투표 마감 날짜는 회식 진행 날짜 이전이어야 합니다.
            - 미완료 회식이 3개 이상이면 생성할 수 없습니다.

            **자동 처리**:
            - 그룹원 전체가 회식 참여자로 자동 등록됩니다.
            - 그룹원의 참/불참 투표 상태가 PENDING(대기중)으로 설정됩니다.
            - 초기 회식 상태는 ATTENDANCE_VOTING(참석 투표 중)입니다.
            """
    )
    @ApiResponse(responseCode = "201", description = "성공")
    @ApiErrorResponses({
        USER_NOT_FOUND,
        GROUP_NOT_FOUND,
        ONLY_GROUP_LEADER_ALLOWED,
        DINING_DATE_MUST_BE_AFTER_NOW,
        VOTE_DUE_DATE_MUST_BE_BEFORE_DINING_DATE,
        DINING_LIMIT_EXCEEDED
    })
    BaseResponse<Long> createDining(
        @Parameter(hidden = true)
        JwtUserDetails user,
        @Parameter(description = "그룹 ID", required = true)
        Long groupId,
        DiningCreateRequest request
    );

    @Operation(
        summary = "회식 상태별 목록 조회",
        description = """
            ### 그룹의 회식 목록을 상태별로 조회합니다.
            - status: ATTENDANCE_VOTING, RESTAURANT_VOTING, CONFIRMED, COMPLETE

            **접근 권한**: 해당 그룹의 그룹장 또는 참여자만 조회 가능
            """
    )
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiErrorResponses({USER_NOT_GROUP_MEMBER})
    BaseResponse<List<DiningResponse>> getDiningList(
        @Parameter(hidden = true)
        JwtUserDetails user,
        @Parameter(description = "그룹 ID", required = true)
        Long groupId,
        @Parameter(description = "회식 상태", required = true)
        DiningStatus status
    );

    @Operation(
        summary = "회식 참석/불참석 투표",
        description = """
            ### 회식 참석/불참석을 투표합니다.
            - votingStatus: ATTEND(참석), NON_ATTEND(불참)

            **투표 조건**:
            - 해당 회식의 참여자만 투표할 수 있습니다.
            - 회식 상태가 ATTENDANCE_VOTING(참석 투표 중)일 때만 투표 가능합니다.
            - 이미 투표한 경우 재투표하여 상태를 변경할 수 있습니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiErrorResponses({NO_VOTE_PERMISSION, DINING_NOT_FOUND, ATTENDANCE_VOTING_CLOSED, INVALID_VOTE_STATUS, ATTENDANCE_VOTE_ALREADY_COMPLETED})
    BaseResponse<VotingStatus> voteAttendance(
        @Parameter(hidden = true)
        JwtUserDetails user,
        @Parameter(description = "그룹 ID", required = true)
        Long groupId,
        @Parameter(description = "회식 ID", required = true)
        Long diningId,
        AttendanceVoteRequest request
    );

    @Operation(
        summary = "추천 식당 투표",
        description = """
            ### 추천 식당에 대해 좋아요/싫어요 투표를 합니다.
            - voteStatus: LIKE(추천), DISLIKE(비추천)

            **투표 조건**:
            - 해당 그룹의 멤버만 투표할 수 있습니다.
            - 회식 상태가 RESTAURANT_VOTING(식당 투표 중)일 때만 투표 가능합니다.

            **투표 변경 로직**:
            - 새 투표: 해당 상태로 투표 생성, count 증가
            - LIKE → DISLIKE: likeCount 감소, dislikeCount 증가
            - DISLIKE → LIKE: dislikeCount 감소, likeCount 증가
            - 같은 투표 시도 시: 투표 삭제, count 감소 (응답의 voteStatus는 요청한 voteStatus와 동일)
            """
    )
    @ApiResponse(responseCode = "201", description = "투표 성공")
    @ApiErrorResponses({
        USER_NOT_GROUP_MEMBER,
        DINING_NOT_FOUND,
        RESTAURANT_VOTING_CLOSED,
        RECOMMEND_RESTAURANT_NOT_FOUND
    })
    BaseResponse<RestaurantVoteResponse> voteRestaurant(
        @Parameter(hidden = true)
        JwtUserDetails user,
        @Parameter(description = "그룹 ID", required = true)
        Long groupId,
        @Parameter(description = "회식 ID", required = true)
        Long diningId,
        @Parameter(description = "추천 식당 ID", required = true)
        Long recommendRestaurantsId,
        RestaurantVoteRequest request
    );
}
