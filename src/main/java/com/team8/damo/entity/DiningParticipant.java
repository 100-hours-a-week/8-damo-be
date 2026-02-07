package com.team8.damo.entity;

import com.team8.damo.entity.enumeration.AttendanceVoteStatus;
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
    name = "dining_participants",
    uniqueConstraints = @UniqueConstraint(
        name = "UK_DINING_PARTICIPANTS",
        columnNames = {"dining_id", "users_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiningParticipant {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "dining_id", nullable = false)
    private Dining dining;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "users_id", nullable = false)
    private User user;

    @Enumerated(STRING)
    @Column(name = "voting_status", nullable = false, length = 20)
    private AttendanceVoteStatus attendanceVoteStatus;

    @Builder
    public DiningParticipant(Long id, Dining dining, User user, AttendanceVoteStatus attendanceVoteStatus) {
        this.id = id;
        this.dining = dining;
        this.user = user;
        this.attendanceVoteStatus = attendanceVoteStatus;
    }

    public static DiningParticipant createPendingParticipant(Long id, Dining dining, User user) {
        return DiningParticipant.builder()
            .id(id)
            .dining(dining)
            .user(user)
            .attendanceVoteStatus(AttendanceVoteStatus.PENDING)
            .build();
    }

    public void updateVotingStatus(AttendanceVoteStatus attendanceVoteStatus) {
        this.attendanceVoteStatus = attendanceVoteStatus;
    }

    public boolean isFirstAttendanceVote() {
        return this.attendanceVoteStatus == AttendanceVoteStatus.PENDING;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiningParticipant that = (DiningParticipant) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
