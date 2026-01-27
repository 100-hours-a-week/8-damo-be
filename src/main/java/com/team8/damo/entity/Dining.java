package com.team8.damo.entity;

import com.team8.damo.entity.enumeration.DiningStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "dining")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dining extends BaseTimeEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "groups_id", nullable = false)
    private Group group;

    @Column(name = "dining_date", nullable = false)
    private LocalDateTime diningDate;

    @Column(name = "vote_due_date", nullable = false)
    private LocalDateTime voteDueDate;

    @Column(name = "budget", nullable = false)
    private Integer budget;

    @Enumerated(STRING)
    @Column(name = "dining_status", nullable = false, length = 20)
    private DiningStatus diningStatus;

    @Column(name = "attendance_vote_done_count")
    private Integer attendanceVoteDoneCount = 0;

    @Builder
    public Dining(Long id, Group group, LocalDateTime diningDate, LocalDateTime voteDueDate, Integer budget, DiningStatus diningStatus) {
        this.id = id;
        this.group = group;
        this.diningDate = diningDate;
        this.voteDueDate = voteDueDate;
        this.budget = budget;
        this.diningStatus = diningStatus;
    }

    public void startRestaurantVoting() {
        this.diningStatus = DiningStatus.RESTAURANT_VOTING;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dining dining = (Dining) o;
        return Objects.equals(id, dining.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
