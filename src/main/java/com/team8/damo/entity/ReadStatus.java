package com.team8.damo.entity;

import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.FetchType.*;

@Entity
@Getter
@Table(name = "read_status")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReadStatus {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "users_id", nullable = false)
    private User user;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "chat_messages_id", nullable = false)
    private ChatMessage chatMessage;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "lightning_gathering", nullable = false)
    private LightningGathering lightningGathering;

    @Builder.Default
    @Column(name = "is_read")
    private boolean isRead = false;
}
