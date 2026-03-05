package com.team8.damo.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

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
public class UserAllergy extends BaseTimeEntity implements Persistable<Long> {

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

    @Override
    public boolean isNew() {
        return getCreatedAt() == null;
    }
}
