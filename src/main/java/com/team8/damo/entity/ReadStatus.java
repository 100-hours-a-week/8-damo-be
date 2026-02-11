package com.team8.damo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import static jakarta.persistence.FetchType.*;

@Entity
@Getter
@Table(name = "read_status")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReadStatus extends BaseTimeEntity implements Persistable<Long> {

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
    @JoinColumn(name = "lightning_id", nullable = false)
    private Lightning lightning;

    @Builder.Default
    @Column(name = "is_read")
    private boolean isRead = false;

    @Override
    public boolean isNew() {
        return getCreatedAt() == null;
    }
}
