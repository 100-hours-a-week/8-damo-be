package com.team8.damo.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseTimeEntity implements Persistable<Long> {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "users_id", nullable = false)
    private User user;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "dining_id", nullable = false)
    private Dining dining;

    @Column(name = "restaurant_id", nullable = false)
    private String restaurantId;

    @Column(name = "star_rating", nullable = false)
    private Integer starRating;

    @Column(name = "content", length = 200)
    private String content;

    @OneToMany(mappedBy = "review", fetch = LAZY)
    private List<ReviewSatisfaction> satisfactionTags = new ArrayList<>();

    @Builder
    public Review(Long id, User user, Dining dining, String restaurantId, Integer starRating, String content) {
        this.id = id;
        this.user = user;
        this.dining = dining;
        this.restaurantId = restaurantId;
        this.starRating = starRating;
        this.content = content;
    }

    @Override
    public boolean isNew() {
        return getCreatedAt() == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Review review = (Review) o;
        return Objects.equals(id, review.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
