package com.team8.damo.entity;

import com.team8.damo.entity.enumeration.NotificationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.util.Objects;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(
    name = "notification",
    indexes = {
        @Index(name = "IDX_NOTIFICATION_USER_ID", columnList = "users_id, id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity implements Persistable<Long> {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "users_id", nullable = false)
    private User user;

    @Enumerated(STRING)
    @Column(name = "notification_type", nullable = false, length = 40)
    private NotificationType notificationType;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "body", nullable = false, length = 500)
    private String body;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    public static Notification create(Long id, User user, NotificationType notificationType, String title, String body) {
        Notification notification = new Notification();
        notification.id = id;
        notification.user = user;
        notification.notificationType = notificationType;
        notification.title = title;
        notification.body = body;
        return notification;
    }

    @Override
    public boolean isNew() {
        return getCreatedAt() == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
