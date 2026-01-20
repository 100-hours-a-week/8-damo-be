package com.team8.damo.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "users_allergies",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UK_USER_ALLERGY",
            columnNames = {"users_id", "allergy_categories_id"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAllergy extends BaseTimeEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allergy_categories_id", nullable = false)
    private AllergyCategory allergyCategory;

    public UserAllergy(Long id, User user, AllergyCategory allergyCategory) {
        this.id = id;
        this.user = user;
        this.allergyCategory = allergyCategory;
    }
}
