package com.team8.damo.repository;

import com.team8.damo.entity.Dining;
import com.team8.damo.entity.enumeration.DiningStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DiningRepository extends JpaRepository<Dining, Long> {

    int countByGroupIdAndDiningStatusNot(Long groupId, DiningStatus status);

    List<Dining> findAllByGroupIdAndDiningStatus(Long groupId, DiningStatus status);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Dining d set d.attendanceVoteDoneCount = d.attendanceVoteDoneCount + 1 where d.id = :diningId")
    void increaseAttendanceVoteDoneCount(@Param("diningId") Long diningId);

    @Modifying(flushAutomatically = true)
    @Query("update Dining d set d.attendanceVoteDoneCount = :count where d.id = :diningId")
    void setAttendanceVoteDoneCount(@Param("diningId") Long diningId, @Param("count") int count);

    @Query("select d.attendanceVoteDoneCount from Dining d where d.id = :diningId")
    int getAttendanceVoteDoneCount(@Param("diningId") Long diningId);

    @Query("select d from Dining d join fetch d.group where d.id = :diningId")
    Optional<Dining> findByIdWithGroup(Long diningId);
}
