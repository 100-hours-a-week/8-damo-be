package com.team8.damo.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.util.Objects;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(
    name = "reviews_satisfactions_categories",
    uniqueConstraints = @UniqueConstraint(columnNames = {"reviews_id", "satisfaction_categories_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewSatisfaction extends BaseTimeEntity implements Persistable<Long> {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "reviews_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "satisfaction_categories_id")
    private SatisfactionCategory satisfactionCategory;

    @Builder
    public ReviewSatisfaction(Long id, Review review, SatisfactionCategory satisfactionCategory) {
        this.id = id;
        this.review = review;
        this.satisfactionCategory = satisfactionCategory;
    }

    @Override
    public boolean isNew() {
        return getCreatedAt() == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReviewSatisfaction that = (ReviewSatisfaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
