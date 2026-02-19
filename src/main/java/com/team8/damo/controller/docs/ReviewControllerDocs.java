package com.team8.damo.controller.docs;

import com.team8.damo.controller.request.ReviewCreateRequest;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.security.jwt.JwtUserDetails;
import com.team8.damo.service.response.ReviewDetailResponse;
import com.team8.damo.service.response.ReviewListItemResponse;
import com.team8.damo.swagger.annotation.ApiErrorResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

import static com.team8.damo.exception.errorcode.ErrorCode.*;

@Tag(name = "Review API", description = "리뷰 관련 API")
public interface ReviewControllerDocs {

    @Operation(
        summary = "리뷰 작성",
        description = """
            ### 확정된 회식에 대한 리뷰를 작성합니다.
            - starRating: 별점 (1~5, 필수)
            - satisfactionTags: 만족 태그 (1개 이상 필수, 중복 불가)
            - content: 텍스트 후기 (선택, 200자 이내)

            **작성 조건**:
            - 회식 상태가 CONFIRMED(확정)인 경우에만 작성 가능합니다.
            - 해당 회식의 참여자만 리뷰를 작성할 수 있습니다.
            - 동일 회식에 여러 번 리뷰 작성이 가능합니다.
            """
    )
    @ApiResponse(responseCode = "201", description = "성공")
    @ApiErrorResponses({
        DINING_NOT_FOUND,
        DINING_NOT_CONFIRMED,
        DINING_PARTICIPANT_REQUIRED_FOR_REVIEW,
        RECOMMEND_RESTAURANT_NOT_FOUND,
        DUPLICATE_SATISFACTION_TAG
    })
    BaseResponse<Long> createReview(
        @Parameter(hidden = true) JwtUserDetails user,
        @Parameter(description = "회식 ID", required = true) Long diningId,
        ReviewCreateRequest request
    );

    @Operation(
        summary = "내 리뷰 목록 조회",
        description = """
            ### 내가 작성한 리뷰 목록을 조회합니다.

            **응답 정보**:
            - reviewId: 리뷰 ID
            - diningId: 회식 ID
            - groupName: 그룹 이름
            - restaurantName: 식당 이름
            - starRating: 별점
            - satisfactions: 만족 태그 목록 (id, category)
            - createdAt: 작성일시
            """
    )
    @ApiResponse(responseCode = "200", description = "성공")
    BaseResponse<List<ReviewListItemResponse>> getMyReviews(
        @Parameter(hidden = true) JwtUserDetails user
    );

    @Operation(
        summary = "리뷰 상세 조회",
        description = """
            ### 특정 리뷰의 상세 정보를 조회합니다.

            **응답 정보**:
            - reviewId: 리뷰 ID
            - diningId: 회식 ID
            - groupName: 그룹 이름
            - restaurantName: 식당 이름
            - starRating: 별점
            - satisfactions: 만족 태그 목록 (id, category)
            - content: 텍스트 후기
            - createdAt: 작성일시

            **접근 권한**: 본인이 작성한 리뷰만 조회 가능
            """
    )
    @ApiResponse(responseCode = "200", description = "성공")
    @ApiErrorResponses({REVIEW_NOT_FOUND})
    BaseResponse<ReviewDetailResponse> getReviewDetail(
        @Parameter(hidden = true) JwtUserDetails user,
        @Parameter(description = "리뷰 ID", required = true) Long reviewId
    );
}
