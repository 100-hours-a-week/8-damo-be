package com.team8.damo.repository;

import com.team8.damo.entity.LightningParticipant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LightningParticipantRepository extends JpaRepository<LightningParticipant, Long> {

    @Query("select lp from LightningParticipant lp " +
        "join fetch lp.user " +
        "where lp.lightning.id = :lightningId")
    List<LightningParticipant> findAllByLightningIdWithUser(Long lightningId);

    @EntityGraph(attributePaths = {"lightning"})
    Optional<LightningParticipant> findByLightningIdAndUserId(Long lightningId, Long userId);

    boolean existsByLightningIdAndUserId(Long lightningId, Long userId);

    long countByLightningId(Long lightningId);

    @Query(
        "select lp from LightningParticipant lp " +
        "join fetch lp.lightning " +
            "where lp.user.id = :userId and lp.lightning.lightningDate >= :cutoffDate "
    )
    List<LightningParticipant> findLightningByUserIdAndCutoffDate(
        @Param("userId") Long userId,
        @Param("cutoffDate") LocalDateTime cutoffDate
    );

    List<LightningParticipant> findAllByLightningIdIn(List<Long> lightningIds);
}
