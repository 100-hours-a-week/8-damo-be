package com.team8.damo.service.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.team8.damo.entity.Review;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewListItemResponse(
    Long reviewId,
    Long diningId,
    String groupName,
    String restaurantName,
    Integer starRating,
    List<CategoryResponse> satisfactions,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    LocalDateTime createdAt
) {
    public static ReviewListItemResponse of(Review review, String restaurantName) {
        List<CategoryResponse> categories = review.getSatisfactionTags().stream()
            .map(rs -> CategoryResponse.from(
                rs.getSatisfactionCategory().getId().intValue(),
                rs.getSatisfactionCategory().getCategory().getDescription()
            ))
            .toList();

        return new ReviewListItemResponse(
            review.getId(),
            review.getDining().getId(),
            review.getDining().getGroup().getName(),
            restaurantName,
            review.getStarRating(),
            categories,
            review.getCreatedAt()
        );
    }
}
