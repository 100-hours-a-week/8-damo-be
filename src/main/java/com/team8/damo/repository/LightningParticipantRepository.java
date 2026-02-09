package com.team8.damo.repository;

import com.team8.damo.entity.LightningParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LightningParticipantRepository extends JpaRepository<LightningParticipant, Long> {

    List<LightningParticipant> findAllByLightningId(Long lightningGatheringId);

    Optional<LightningParticipant> findByLightningIdAndUserId(Long lightningGatheringId, Long userId);
}
