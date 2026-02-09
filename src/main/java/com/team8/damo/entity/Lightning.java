package com.team8.damo.entity;

import com.team8.damo.entity.enumeration.LightningStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "lightning_gathering")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lightning extends BaseTimeEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private String restaurantId;

    @Column(name = "max_participants", nullable = false)
    private int maxParticipants;

    @Column(name = "description", length = 30)
    private String description;

    @Column(name = "gathering_status")
    @Enumerated(EnumType.STRING)
    private LightningStatus lightningStatus;

    @Column(name = "lightning_date", nullable = false)
    private LocalDateTime lightningDate;

    @Builder
    public Lightning(Long id, String restaurantId, int maxParticipants, String description, LocalDateTime lightningDate) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.maxParticipants = maxParticipants;
        this.description = description;
        this.lightningDate = lightningDate;
        this.lightningStatus = LightningStatus.OPEN;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lightning that = (Lightning) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
