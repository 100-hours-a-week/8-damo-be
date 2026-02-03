package com.team8.damo.service;

import com.team8.damo.entity.Dining;
import com.team8.damo.entity.RecommendRestaurant;
import com.team8.damo.exception.CustomException;
import com.team8.damo.exception.errorcode.ErrorCode;
import com.team8.damo.repository.DiningRepository;
import com.team8.damo.repository.RecommendRestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendRestaurantService {

    private final DiningRepository diningRepository;
    private final RecommendRestaurantRepository recommendRestaurantRepository;

    @Transactional
    public void updateRecommendRestaurant(Long diningId, int recommendationCount, List<RecommendRestaurant> recommendRestaurants) {
        recommendRestaurantRepository.saveAll(recommendRestaurants);

        Dining dining = diningRepository.findById(diningId)
            .orElseThrow(() -> new CustomException(ErrorCode.DINING_NOT_FOUND));
        dining.startRestaurantVoting();
        dining.changeRecommendationCount(recommendationCount);

        log.info("recommendationCount: {}", recommendationCount);
        recommendRestaurants.forEach(recommendRestaurant -> {
            log.info("recommendRestaurant: {} {}",  recommendRestaurant.getRestaurantId(), recommendRestaurant.getReasoningDescription());
        });
    }
}
