package com.team8.damo.entity;

import com.team8.damo.entity.enumeration.VoteStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(
    name = "recommend_restaurants_vote",
    uniqueConstraints = @UniqueConstraint(
        name = "UK_RECOMMEND_RESTAURANTS_VOTE",
        columnNames = {"users_id", "recommend_restaurants_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendRestaurantVote {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "users_id", nullable = false)
    private User user;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "recommend_restaurants_id", nullable = false)
    private RecommendRestaurant recommendRestaurant;

    @Enumerated(STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VoteStatus status;

    @Builder
    public RecommendRestaurantVote(Long id, User user, RecommendRestaurant recommendRestaurant, VoteStatus status) {
        this.id = id;
        this.user = user;
        this.recommendRestaurant = recommendRestaurant;
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecommendRestaurantVote that = (RecommendRestaurantVote) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
