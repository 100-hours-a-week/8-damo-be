package com.team8.damo.entity;

import com.team8.damo.entity.enumeration.GatheringRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.springframework.data.domain.Persistable;

import java.util.Objects;

import static com.team8.damo.entity.enumeration.GatheringRole.*;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "lightning_participants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LightningParticipant extends BaseTimeEntity implements Persistable<Long> {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "lightning_id", nullable = false)
    private Lightning lightning;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "users_id", nullable = false)
    private User user;

    @Enumerated(STRING)
    @Column(name = "role", nullable = false, length = 20)
    private GatheringRole role;

    @Column(name = "last_read_chat_messages_id")
    private Long lastReadChatMessageId;

    @Builder
    public LightningParticipant(Long id, Lightning lightning, User user, GatheringRole role) {
        this.id = id;
        this.lightning = lightning;
        this.user = user;
        this.role = role;
    }

    public static LightningParticipant createLeader(Long id, Lightning lightning, User user) {
        return LightningParticipant.builder()
            .id(id)
            .lightning(lightning)
            .user(user)
            .role(LEADER)
            .build();
    }

    public static LightningParticipant createParticipant(Long id, Lightning lightning, User user) {
        return LightningParticipant.builder()
            .id(id)
            .lightning(lightning)
            .user(user)
            .role(PARTICIPANT)
            .build();
    }

    public void updateLastReadChatMessageId(Long messageId) {
        this.lastReadChatMessageId = messageId;
    }

    public boolean isNotLeader() {
        return this.role != LEADER;
    }

    @Override
    public boolean isNew() {
        return getCreatedAt() == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LightningParticipant that = (LightningParticipant) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
