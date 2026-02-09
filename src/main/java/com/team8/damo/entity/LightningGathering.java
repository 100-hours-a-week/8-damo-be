package com.team8.damo.entity;

import com.team8.damo.entity.enumeration.GatheringStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(name = "lightning_gathering")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LightningGathering extends BaseTimeEntity {

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
    private GatheringStatus gatheringStatus;

    @Builder
    public LightningGathering(Long id, String restaurantId, int maxParticipants, String description) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.maxParticipants = maxParticipants;
        this.description = description;
        this.gatheringStatus = GatheringStatus.OPEN;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LightningGathering that = (LightningGathering) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
