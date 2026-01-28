package com.team8.damo.repository;

import com.team8.damo.entity.RecommendRestaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecommendRestaurantRepository extends JpaRepository<RecommendRestaurant, Long> {

    List<RecommendRestaurant> findByDiningIdAndRecommendationCount(Long diningId, Integer recommendationCount);

    @Modifying(flushAutomatically = true)
    @Query("update RecommendRestaurant r set r.likeCount = r.likeCount + 1 where r.id = :id")
    void increaseLikeCount(@Param("id") Long id);

    @Modifying(flushAutomatically = true)
    @Query("update RecommendRestaurant r set r.likeCount = r.likeCount - 1 where r.id = :id and r.likeCount > 0")
    void decreaseLikeCount(@Param("id") Long id);

    @Modifying(flushAutomatically = true)
    @Query("update RecommendRestaurant r set r.dislikeCount = r.dislikeCount + 1 where r.id = :id")
    void increaseDislikeCount(@Param("id") Long id);

    @Modifying(flushAutomatically = true)
    @Query("update RecommendRestaurant r set r.dislikeCount = r.dislikeCount - 1 where r.id = :id and r.dislikeCount > 0")
    void decreaseDislikeCount(@Param("id") Long id);

    @Modifying(flushAutomatically = true)
    @Query("update RecommendRestaurant r set r.likeCount = :likeCount, r.dislikeCount = :dislikeCount where r.id = :id")
    void setVoteCounts(@Param("id") Long id, @Param("likeCount") int likeCount, @Param("dislikeCount") int dislikeCount);
}
