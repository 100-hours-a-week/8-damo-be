package com.team8.damo.repository;

import com.team8.damo.entity.Lightning;
import com.team8.damo.entity.enumeration.LightningStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LightningRepository extends JpaRepository<Lightning, Long> {

    List<Lightning> findAllByLightningStatus(LightningStatus status);

    @Query(
        "SELECT l.id FROM Lightning l " +
            "WHERE l.description LIKE CONCAT(:prefix, '%') " +
            "ORDER BY l.id DESC"
    )
    List<Long> findIdsByDescriptionPrefix(@Param("prefix") String prefix);

    @Query("SELECT l FROM Lightning l WHERE l.description LIKE CONCAT(:prefix, '%')")
    List<Lightning> findAllByDescriptionPrefix(@Param("prefix") String prefix);

    @Query(
        "SELECT l FROM Lightning l " +
            "WHERE l.lightningStatus = :status " +
            "AND (" +
            "SELECT COUNT(lp) FROM LightningParticipant lp " +
            "WHERE lp.lightning = l" +
            ") < l.maxParticipants " +
            "AND NOT EXISTS (" +
            "SELECT 1 FROM LightningParticipant lp " +
            "WHERE lp.lightning = l AND lp.user.id = :userId" +
            ")" +
            "ORDER BY l.id DESC"
    )
    List<Lightning> findAllByStatusAndUserNotParticipating(
        @Param("status") LightningStatus status,
        @Param("userId") Long userId
    );

    @Query(
        "SELECT l FROM Lightning l " +
            "WHERE l.lightningStatus = :status " +
            "AND NOT EXISTS (" +
            "SELECT 1 FROM LightningParticipant lp " +
            "WHERE lp.lightning = l AND lp.user.id = :userId" +
            ") " +
            "ORDER BY l.id DESC"
    )
    List<Lightning> findAllByStatusAndUserNotParticipatingWithCursor(
        @Param("status") LightningStatus status,
        @Param("userId") Long userId,
        Pageable pageable
    );

    @Query(
        "SELECT l FROM Lightning l " +
            "WHERE l.lightningStatus = :status " +
            "AND l.id < :lastLightningId " +
            "AND (" +
            "SELECT COUNT(lp) FROM LightningParticipant lp " +
            "WHERE lp.lightning = l" +
            ") < l.maxParticipants " +
            "AND NOT EXISTS (" +
            "SELECT 1 FROM LightningParticipant lp " +
            "WHERE lp.lightning = l AND lp.user.id = :userId" +
            ") " +
            "ORDER BY l.id DESC"
    )
    List<Lightning> findAllByStatusAndUserNotParticipatingWithCursorAfter(
        @Param("status") LightningStatus status,
        @Param("userId") Long userId,
        @Param("lastLightningId") Long lastLightningId,
        Pageable pageable
    );
}
