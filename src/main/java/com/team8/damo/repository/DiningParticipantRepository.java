package com.team8.damo.repository;

import com.team8.damo.entity.Dining;
import com.team8.damo.entity.DiningParticipant;
import com.team8.damo.entity.enumeration.AttendanceVoteStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DiningParticipantRepository extends JpaRepository<DiningParticipant, Long> {

    int countByDiningIdAndAttendanceVoteStatus(Long diningId, AttendanceVoteStatus attendanceVoteStatus);

    List<Integer> countByDiningIdInAndAttendanceVoteStatus(List<Long> diningIds, AttendanceVoteStatus attendanceVoteStatus);

    Optional<DiningParticipant> findByDiningIdAndUserId(Long diningId, Long userId);

    int countByDiningId(Long diningId);

    @EntityGraph(attributePaths = {"dining"})
    List<DiningParticipant> findByDiningIdInAndAttendanceVoteStatus(
        List<Long> diningIds,
        AttendanceVoteStatus attendanceVoteStatus
    );

    @EntityGraph(attributePaths = {"user"})
    List<DiningParticipant> findAllByDiningAndAttendanceVoteStatus(Dining dining, AttendanceVoteStatus attendanceVoteStatus);

    @Query("SELECT dp FROM DiningParticipant dp JOIN FETCH dp.user WHERE dp.dining.id = :diningId AND dp.attendanceVoteStatus = 'ATTEND'")
    List<DiningParticipant> findAttendingParticipantsWithUser(@Param("diningId") Long diningId);
}
