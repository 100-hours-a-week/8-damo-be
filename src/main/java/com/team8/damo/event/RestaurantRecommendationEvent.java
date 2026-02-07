package com.team8.damo.event;

import com.team8.damo.entity.Dining;
import com.team8.damo.entity.Group;

import java.util.List;

/**
 * 참석 투표 완료 시 AI 식당 추천을 요청하기 위한 이벤트
 *
 * @param group   회식이 속한 그룹
 * @param dining  회식 정보
 * @param userIds 참석 투표한 사용자 ID 목록
 */
public record RestaurantRecommendationEvent(
    Group group,
    Dining dining,
    List<Long> userIds
) {

    public static RestaurantRecommendationEvent of(Group group, Dining dining, List<Long> userIds) {
        return new RestaurantRecommendationEvent(group, dining, List.copyOf(userIds));
    }
}
