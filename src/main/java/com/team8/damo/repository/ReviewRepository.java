package com.team8.damo.repository;

import com.team8.damo.entity.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = {"dining", "dining.group"})
    List<Review> findAllByUserId(Long userId);

    @EntityGraph(attributePaths = {"dining", "dining.group"})
    Optional<Review> findByIdAndUserId(Long id, Long userId);
}
