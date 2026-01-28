package com.team8.damo.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "recommend_restaurants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendRestaurant extends BaseTimeEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "dining_id", nullable = false)
    private Dining dining;

    @Column(name = "restaurants_id", nullable = false)
    private String restaurantId;

    @Column(name = "confirmed_status", nullable = false)
    private Boolean confirmedStatus = false;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;

    @Column(name = "dislike_count", nullable = false)
    private Integer dislikeCount = 0;

    @Column(name = "point", nullable = false)
    private Integer point;

    @Column(name = "reasoning_description", nullable = false, length = 500)
    private String reasoningDescription;

    @Column(name = "recommendation_count")
    private Integer recommendationCount;

    @Builder
    public RecommendRestaurant(Long id, Dining dining, String restaurantId, Boolean confirmedStatus, Integer likeCount, Integer dislikeCount, Integer point, String reasoningDescription,  Integer recommendationCount) {
        this.id = id;
        this.dining = dining;
        this.restaurantId = restaurantId;
        this.confirmedStatus = confirmedStatus != null ? confirmedStatus : false;
        this.likeCount = likeCount != null ? likeCount : 0;
        this.dislikeCount = dislikeCount != null ? dislikeCount : 0;
        this.point = point;
        this.reasoningDescription = reasoningDescription;
        this.recommendationCount = recommendationCount;
    }

    public void confirmed() {
        this.confirmedStatus = true;
    }

    public boolean isConfirmed() {
        return this.confirmedStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecommendRestaurant that = (RecommendRestaurant) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
