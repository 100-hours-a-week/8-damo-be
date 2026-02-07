package com.team8.damo.controller.docs;

import com.team8.damo.controller.request.AttendanceVoteRequest;
import com.team8.damo.controller.request.DiningCreateRequest;
import com.team8.damo.controller.request.RestaurantVoteRequest;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.entity.enumeration.AttendanceVoteStatus;
import com.team8.damo.entity.enumeration.DiningStatus;
import com.team8.damo.security.jwt.JwtUserDetails;
import com.team8.damo.service.response.AttendanceVoteDetailResponse;
import com.team8.damo.service.response.DiningConfirmedResponse;
import com.team8.damo.service.response.DiningDetailResponse;
import com.team8.damo.service.response.DiningResponse;
import com.team8.damo.service.response.RestaurantVoteDetailResponse;
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
        summary = "회식 상세 조회(공통)",
        description = """
            ### 특정 회식의 상세 정보를 조회합니다.

            **응답 정보**:
            - isGroupLeader: 요청자의 그룹장 여부
            - diningDate: 회식 날짜
            - diningStatus: 회식 상태 (ATTENDANCE_VOTING, RESTAURANT_VOTING, RECOMMENDATION_PENDING, CONFIRMED, COMPLETE)
            - diningParticipants: 참석 투표한 참여자 목록 (ATTEND 상태만)

            **접근 권한**: 해당 그룹의 그룹원만 조회 가능
            """
    )
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiErrorResponses({USER_NOT_GROUP_MEMBER, DINING_NOT_FOUND})
    BaseResponse<DiningDetailResponse> getDiningDetail(
        @Parameter(hidden = true)
        JwtUserDetails user,
        @Parameter(description = "그룹 ID", required = true)
        Long groupId,
        @Parameter(description = "회식 ID", required = true)
        Long diningId
    );

    @Operation(
        summary = "회식 상세 조회(참석/불참석 투표)",
        description = """
            ### 특정 회식의 참석/불참석 투표 현황을 조회합니다.

            **응답 정보**:
            - attendanceVoteStatus: 요청자의 투표 상태 (PENDING/ATTEND/NON_ATTEND)
            - completedVoteCount: 투표 완료된 인원 수
            - totalGroupMemberCount: 그룹 전체 멤버 수

            **접근 권한**: 해당 그룹의 그룹원만 조회 가능
            """
    )
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiErrorResponses({USER_NOT_GROUP_MEMBER, DINING_NOT_FOUND, NO_VOTE_PERMISSION})
    BaseResponse<AttendanceVoteDetailResponse> getAttendanceVoteDetail(
        @Parameter(hidden = true)
        JwtUserDetails user,
        @Parameter(description = "그룹 ID", required = true)
        Long groupId,
        @Parameter(description = "회식 ID", required = true)
        Long diningId
    );

    @Operation(
        summary = "회식 참석/불참석 투표",
        description = """
            ### 회식 참석/불참석을 투표합니다.
            - attendanceVoteStatus: ATTEND(참석), NON_ATTEND(불참)

            **투표 조건**:
            - 해당 회식의 참여자만 투표할 수 있습니다.
            - 회식 상태가 ATTENDANCE_VOTING(참석 투표 중)일 때만 투표 가능합니다.
            - 이미 투표한 경우 재투표하여 상태를 변경할 수 있습니다.
            """
    )
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiErrorResponses({NO_VOTE_PERMISSION, DINING_NOT_FOUND, ATTENDANCE_VOTING_CLOSED, INVALID_VOTE_STATUS})
    BaseResponse<AttendanceVoteStatus> voteAttendance(
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
            - restaurantVoteStatus: LIKE(추천), DISLIKE(비추천)

            **투표 조건**:
            - 해당 그룹의 멤버만 투표할 수 있습니다.
            - 회식 상태가 RESTAURANT_VOTING(식당 투표 중)일 때만 투표 가능합니다.

            **투표 변경 로직**:
            - 새 투표: 해당 상태로 투표 생성, count 증가, 요청 보낸 상태와 동일한 상태 반환
            - LIKE → DISLIKE: likeCount 감소, dislikeCount 증가 (DISLIKE 반환)
            - DISLIKE → LIKE: dislikeCount 감소, likeCount 증가  (LIKE 반환)
            - 같은 투표 시도 시: 투표 삭제, count 감소 (NONE 반환)
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

    @Operation(
        summary = "회식 상세 조회(장소 투표)",
        description = """
            ### 특정 회식의 장소 투표 현황을 조회합니다.

            **응답 정보**:
            - recommendRestaurantsId: 추천 식당 ID
            - restaurantsName: 식당 이름
            - reasoningDescription: AI 추천 이유
            - restaurantVoteStatus: 요청자의 투표 상태 (LIKE/DISLIKE/NONE)
            - phoneNumber: 식당 전화번호
            - latitude: 위도
            - longitude: 경도
            - likeCount: 추천 수
            - dislikeCount: 비추천 수

            **접근 권한**: 해당 그룹의 그룹원만 조회 가능
            """
    )
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiErrorResponses({USER_NOT_GROUP_MEMBER, DINING_NOT_FOUND, NO_VOTE_PERMISSION})
    BaseResponse<List<RestaurantVoteDetailResponse>> getRestaurantVoteDetail(
        @Parameter(hidden = true)
        JwtUserDetails user,
        @Parameter(description = "그룹 ID", required = true)
        Long groupId,
        @Parameter(description = "회식 ID", required = true)
        Long diningId
    );

    @Operation(
        summary = "회식 상세 조회(장소 확정)",
        description = """
            ### 확정된 회식 장소 정보를 조회합니다.

            **응답 정보**:
            - recommendRestaurantsId: 확정된 추천 식당 ID
            - restaurantsName: 식당 이름
            - reasoningDescription: AI 추천 이유
            - phoneNumber: 식당 전화번호
            - latitude: 위도
            - longitude: 경도

            **접근 권한**: 해당 그룹의 그룹원만 조회 가능

            **조회 조건**: 장소가 확정된 회식만 조회 가능
            """
    )
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiErrorResponses({USER_NOT_GROUP_MEMBER, DINING_NOT_FOUND, RECOMMEND_RESTAURANT_NOT_FOUND, RESTAURANT_NOT_FOUND})
    BaseResponse<DiningConfirmedResponse> getDiningConfirmed(
        @Parameter(hidden = true)
        JwtUserDetails user,
        @Parameter(description = "그룹 ID", required = true)
        Long groupId,
        @Parameter(description = "회식 ID", required = true)
        Long diningId
    );

    @Operation(
        summary = "회식 장소 확정하기",
        description = """
            ### 그룹장이 추천된 식당 중 하나를 회식 장소로 확정합니다.

            **확정 조건**:
            - 그룹장만 장소를 확정할 수 있습니다.
            - 이미 확정된 식당은 다시 확정할 수 없습니다.
            - 한 회식에 하나의 식당만 확정 가능합니다.

            **상태 변경**:
            - RecommendRestaurant: `confirmedStatus = true`
            - Dining: `diningStatus = CONFIRMED`
            """
    )
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiErrorResponses({
        ONLY_GROUP_LEADER_CAN_CONFIRM,
        DINING_NOT_FOUND,
        RECOMMEND_RESTAURANT_NOT_FOUND,
        RESTAURANT_NOT_FOUND,
        RECOMMEND_RESTAURANT_ALREADY_CONFIRMED,
        ANOTHER_RESTAURANT_ALREADY_CONFIRMED
    })
    BaseResponse<DiningConfirmedResponse> confirmDiningRestaurant(
        @Parameter(hidden = true)
        JwtUserDetails user,
        @Parameter(description = "그룹 ID", required = true)
        Long groupId,
        @Parameter(description = "회식 ID", required = true)
        Long diningId,
        @Parameter(description = "추천 식당 ID", required = true)
        Long recommendRestaurantsId
    );

    @Operation(
        summary = "장소 재추천 받기",
        description = """
            ### AI를 통해 새로운 식당 추천을 받습니다.
            **호출 후 RECOMMENDATION_PENDING 상태로 변경됩니다.**

            **호출 조건**:
            - 그룹장만 재추천을 요청할 수 있습니다.
            - 기존 투표 결과를 바탕으로 AI가 새로운 추천을 생성합니다.

            **응답 정보(없음)**:
            - recommendRestaurantsId: 추천 식당 ID
            - restaurantsName: 식당 이름
            - reasoningDescription: AI 추천 이유
            - restaurantVoteStatus: 요청자의 투표 상태 (새 추천이므로 NONE)
            - phoneNumber: 식당 전화번호
            - latitude/longitude: 좌표
            - likeCount/dislikeCount: 투표 수 (새 추천이므로 0)
            """
    )
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiErrorResponses({
        ONLY_GROUP_LEADER_CAN_CONFIRM,
        DINING_NOT_FOUND,
        GROUP_NOT_FOUND,
        RESTAURANT_NOT_FOUND
    })
    BaseResponse<Void> refreshRecommendRestaurants(
        @Parameter(hidden = true)
        JwtUserDetails user,
        @Parameter(description = "그룹 ID", required = true)
        Long groupId,
        @Parameter(description = "회식 ID", required = true)
        Long diningId
    );
}
