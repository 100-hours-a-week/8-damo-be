package com.team8.damo.repository;

import com.team8.damo.entity.ReviewSatisfaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewSatisfactionRepository extends JpaRepository<ReviewSatisfaction, Long> {

    @Query(
        "select rs from ReviewSatisfaction rs " +
        "join fetch rs.satisfactionCategory " +
        "where rs.review.id = :reviewId"
    )
    List<ReviewSatisfaction> findByReviewIdWithCategory(@Param("reviewId") Long reviewId);
}
