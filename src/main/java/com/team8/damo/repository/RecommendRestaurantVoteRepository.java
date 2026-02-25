package com.team8.damo.repository;

import com.team8.damo.entity.RecommendRestaurantVote;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RecommendRestaurantVoteRepository extends JpaRepository<RecommendRestaurantVote, Long> {

    Optional<RecommendRestaurantVote> findByUserIdAndRecommendRestaurantId(Long userId, Long recommendRestaurantId);

    @EntityGraph(attributePaths = {"user"})
    List<RecommendRestaurantVote> findByRecommendRestaurantId(Long recommendRestaurantId);

    @EntityGraph(attributePaths = {"user", "recommendRestaurant"})
    List<RecommendRestaurantVote> findByRecommendRestaurantIdIn(Set<Long> recommendRestaurantIds);
}
