package com.team8.damo.service.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.team8.damo.entity.Review;
import com.team8.damo.entity.ReviewSatisfaction;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewDetailResponse(
    Long reviewId,
    Long diningId,
    String groupName,
    String restaurantName,
    Integer starRating,
    List<CategoryResponse> satisfactions,
    String content,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    LocalDateTime createdAt
) {
    public static ReviewDetailResponse of(Review review, String restaurantName, List<ReviewSatisfaction> reviewSatisfactions) {
        List<CategoryResponse> categories = reviewSatisfactions.stream()
            .map(rs -> CategoryResponse.from(
                rs.getSatisfactionCategory().getId().intValue(),
                rs.getSatisfactionCategory().getCategory().getDescription()
            ))
            .toList();

        return new ReviewDetailResponse(
            review.getId(),
            review.getDining().getId(),
            review.getDining().getGroup().getName(),
            restaurantName,
            review.getStarRating(),
            categories,
            review.getContent(),
            review.getCreatedAt()
        );
    }
}
