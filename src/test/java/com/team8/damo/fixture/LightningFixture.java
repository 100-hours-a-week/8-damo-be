package com.team8.damo.fixture;

import com.team8.damo.entity.Lightning;

import java.time.LocalDateTime;

public class LightningFixture {

    public static Lightning create(Long id, String restaurantId) {
        return Lightning.builder()
            .id(id)
            .restaurantId(restaurantId)
            .maxParticipants(4)
            .description("같이 밥 먹어요")
            .lightningDate(LocalDateTime.of(2025, 1, 2, 18, 0))
            .build();
    }

    public static Lightning create(Long id, String restaurantId, LocalDateTime lightningDate) {
        return Lightning.builder()
            .id(id)
            .restaurantId(restaurantId)
            .maxParticipants(4)
            .description("같이 밥 먹어요")
            .lightningDate(lightningDate)
            .build();
    }
}
