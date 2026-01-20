package com.team8.damo.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "users_like_ingredients",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UK_USER_LIKE_INGREDIENT",
            columnNames = {"users_id", "like_ingredient_categories_id"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserLikeIngredient extends BaseTimeEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "like_ingredient_categories_id", nullable = false)
    private LikeIngredientCategory likeIngredientCategory;

    public UserLikeIngredient(Long id, User user, LikeIngredientCategory likeIngredientCategory) {
        this.id = id;
        this.user = user;
        this.likeIngredientCategory = likeIngredientCategory;
    }
}
