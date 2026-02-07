package com.team8.damo.entity;

import com.team8.damo.entity.enumeration.FoodType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.EnumType.STRING;

@Entity
@Table(name = "like_food_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LikeFoodCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(STRING)
    @Column(name = "category", nullable = false, length = 20, unique = true)
    private FoodType category;

    public LikeFoodCategory(FoodType category) {
        this.category = category;
    }
}
