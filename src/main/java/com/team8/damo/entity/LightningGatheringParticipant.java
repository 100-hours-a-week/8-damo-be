package com.team8.damo.entity;

import com.team8.damo.entity.enumeration.GatheringRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

import static com.team8.damo.entity.enumeration.GatheringRole.*;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "lightning_gathering_participants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LightningGatheringParticipant extends BaseTimeEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "lightning_gathering_id", nullable = false)
    private LightningGathering lightningGathering;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "users_id", nullable = false)
    private User user;

    @Enumerated(STRING)
    @Column(name = "role", nullable = false, length = 20)
    private GatheringRole role;

    @Column(name = "last_read_chat_messages_id")
    private Long lastReadChatMessageId;

    @Builder
    public LightningGatheringParticipant(Long id, LightningGathering lightningGathering, User user, GatheringRole role) {
        this.id = id;
        this.lightningGathering = lightningGathering;
        this.user = user;
        this.role = role;
    }

    public static LightningGatheringParticipant createLeader(Long id, LightningGathering lightningGathering, User user) {
        return LightningGatheringParticipant.builder()
            .id(id)
            .lightningGathering(lightningGathering)
            .user(user)
            .role(LEADER)
            .build();
    }

    public static LightningGatheringParticipant createParticipant(Long id, LightningGathering lightningGathering, User user) {
        return LightningGatheringParticipant.builder()
            .id(id)
            .lightningGathering(lightningGathering)
            .user(user)
            .role(PARTICIPANT)
            .build();
    }

    public void updateLastReadChatMessageId(Long messageId) {
        this.lastReadChatMessageId = messageId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LightningGatheringParticipant that = (LightningGatheringParticipant) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
