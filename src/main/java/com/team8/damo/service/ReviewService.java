package com.team8.damo.service;

import com.team8.damo.entity.*;
import com.team8.damo.entity.enumeration.SatisfactionType;
import com.team8.damo.exception.CustomException;
import com.team8.damo.repository.*;
import com.team8.damo.service.request.ReviewCreateServiceRequest;
import com.team8.damo.service.response.ReviewDetailResponse;
import com.team8.damo.service.response.ReviewListItemResponse;
import com.team8.damo.util.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.team8.damo.exception.errorcode.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final Snowflake snowflake;
    private final UserRepository userRepository;
    private final DiningRepository diningRepository;
    private final DiningParticipantRepository diningParticipantRepository;
    private final RecommendRestaurantRepository recommendRestaurantRepository;
    private final RestaurantRepository restaurantRepository;
    private final ReviewRepository reviewRepository;
    private final SatisfactionCategoryRepository satisfactionCategoryRepository;
    private final ReviewSatisfactionRepository reviewSatisfactionRepository;

    @Transactional
    public Long createReview(Long userId, Long diningId, ReviewCreateServiceRequest request) {
        User user = findUserBy(userId);
        Dining dining = findDiningBy(diningId);

        validateDiningConfirmed(dining);
        validateDiningParticipant(diningId, userId);
        validateNoDuplicateTags(request.satisfactions());

        RecommendRestaurant confirmedRestaurant = recommendRestaurantRepository
            .findConfirmedRecommendRestaurant(diningId, dining.getRecommendationCount())
            .orElseThrow(() -> new CustomException(RECOMMEND_RESTAURANT_NOT_FOUND));

        Review review = Review.builder()
            .id(snowflake.nextId())
            .user(user)
            .dining(dining)
            .restaurantId(confirmedRestaurant.getRestaurantId())
            .starRating(request.starRating())
            .content(request.content())
            .build();

        List<SatisfactionCategory> categories = satisfactionCategoryRepository.findByCategoryIn(request.satisfactions());

        List<ReviewSatisfaction> reviewSatisfactionCategories = categories.stream()
            .map(category -> ReviewSatisfaction.builder()
                .id(snowflake.nextId())
                .review(review)
                .satisfactionCategory(category)
                .build()
            )
            .toList();

        reviewRepository.save(review);
        reviewSatisfactionRepository.saveAll(reviewSatisfactionCategories);

        return review.getId();
    }

    public List<ReviewListItemResponse> getMyReviews(Long userId) {
        List<Review> reviews = reviewRepository.findAllByUserId(userId);

        List<String> restaurantIds = reviews.stream()
            .map(Review::getRestaurantId)
            .toList();

        Map<String, String> restaurantNameMap = createRestaurantNameMap(restaurantIds);

        return reviews.stream()
            .map(review -> ReviewListItemResponse.of(
                    review,
                    restaurantNameMap.get(review.getRestaurantId())
                )
            )
            .toList();
    }

    public ReviewDetailResponse getReviewDetail(Long userId, Long reviewId) {
        Review review = reviewRepository.findByIdAndUserId(reviewId, userId)
            .orElseThrow(() -> new CustomException(REVIEW_NOT_FOUND));

        List<ReviewSatisfaction> reviewSatisfactions = reviewSatisfactionRepository.findByReviewIdWithCategory(reviewId);

        String restaurantName = restaurantRepository.findById(review.getRestaurantId())
            .map(Restaurant::getPlaceName)
            .orElse(null);

        return ReviewDetailResponse.of(review, restaurantName, reviewSatisfactions);
    }

    private void validateDiningConfirmed(Dining dining) {
        if (dining.isNotRestaurantConfirmed()) {
            throw new CustomException(DINING_NOT_CONFIRMED);
        }
    }

    private void validateDiningParticipant(Long diningId, Long userId) {
        diningParticipantRepository.findByDiningIdAndUserId(diningId, userId)
            .orElseThrow(() -> new CustomException(DINING_PARTICIPANT_REQUIRED_FOR_REVIEW));
    }

    private void validateNoDuplicateTags(List<SatisfactionType> tags) {
        Set<SatisfactionType> uniqueTags = new HashSet<>(tags);
        if (uniqueTags.size() != tags.size()) {
            throw new CustomException(DUPLICATE_SATISFACTION_TAG);
        }
    }

    private User findUserBy(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }

    private Dining findDiningBy(Long diningId) {
        return diningRepository.findById(diningId)
            .orElseThrow(() -> new CustomException(DINING_NOT_FOUND));
    }

    private Map<String, String> createRestaurantNameMap(List<String> restaurantIds) {
        return restaurantRepository.findAllById(restaurantIds)
            .stream()
            .collect(Collectors.toMap(
                Restaurant::getId,
                Restaurant::getPlaceName
            ));
    }
}
