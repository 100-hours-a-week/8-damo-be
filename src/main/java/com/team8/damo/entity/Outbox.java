package com.team8.damo.entity;

import com.team8.damo.event.EventType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
    name = "outbox",
    indexes = {
        @Index(name = "idx_outbox_created_at", columnList = "createdAt")
    }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Outbox {
    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String payload;
    
    private LocalDateTime createdAt;

    public static Outbox create(Long id, EventType eventType, String payload) {
        Outbox outbox = new Outbox();
        outbox.id = id;
        outbox.eventType = eventType;
        outbox.payload = payload;
        outbox.createdAt = LocalDateTime.now();
        return outbox;
    }
}
