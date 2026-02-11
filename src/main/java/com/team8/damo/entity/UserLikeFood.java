package com.team8.damo.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

@Entity
@Table(
    name = "users_like_foods",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UK_USER_LIKE_FOOD",
            columnNames = {"users_id", "like_food_categories_id"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserLikeFood extends BaseTimeEntity implements Persistable<Long> {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "like_food_categories_id", nullable = false)
    private LikeFoodCategory likeFoodCategory;

    public UserLikeFood(Long id, User user, LikeFoodCategory likeFoodCategory) {
        this.id = id;
        this.user = user;
        this.likeFoodCategory = likeFoodCategory;
    }

    @Override
    public boolean isNew() {
        return getCreatedAt() == null;
    }
}
