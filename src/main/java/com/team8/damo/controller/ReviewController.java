package com.team8.damo.controller;

import com.team8.damo.controller.docs.ReviewControllerDocs;
import com.team8.damo.controller.request.ReviewCreateRequest;
import com.team8.damo.controller.response.BaseResponse;
import com.team8.damo.security.jwt.JwtUserDetails;
import com.team8.damo.service.ReviewService;
import com.team8.damo.service.response.ReviewDetailResponse;
import com.team8.damo.service.response.ReviewListItemResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ReviewController implements ReviewControllerDocs {

    private final ReviewService reviewService;

    @Override
    @PostMapping("/dining/{diningId}/reviews")
    public BaseResponse<Long> createReview(
        @AuthenticationPrincipal JwtUserDetails user,
        @PathVariable Long diningId,
        @Valid @RequestBody ReviewCreateRequest request
    ) {
        return BaseResponse.created(
            reviewService.createReview(user.getUserId(), diningId, request.toServiceRequest())
        );
    }

    @Override
    @GetMapping("/users/me/reviews")
    public BaseResponse<List<ReviewListItemResponse>> getMyReviews(
        @AuthenticationPrincipal JwtUserDetails user
    ) {
        return BaseResponse.ok(
            reviewService.getMyReviews(user.getUserId())
        );
    }

    @Override
    @GetMapping("/users/me/reviews/{reviewId}")
    public BaseResponse<ReviewDetailResponse> getReviewDetail(
        @AuthenticationPrincipal JwtUserDetails user,
        @PathVariable Long reviewId
    ) {
        return BaseResponse.ok(
            reviewService.getReviewDetail(user.getUserId(), reviewId)
        );
    }
}
