package com.team8.damo.repository;

import com.team8.damo.entity.RecommendRestaurantVote;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecommendRestaurantVoteRepository extends JpaRepository<RecommendRestaurantVote, Long> {

    Optional<RecommendRestaurantVote> findByUserIdAndRecommendRestaurantId(Long userId, Long recommendRestaurantId);

    @EntityGraph(attributePaths = {"user"})
    List<RecommendRestaurantVote> findByRecommendRestaurantId(Long recommendRestaurantId);
}
