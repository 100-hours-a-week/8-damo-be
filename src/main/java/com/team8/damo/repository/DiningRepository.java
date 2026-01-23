package com.team8.damo.repository;

import com.team8.damo.entity.Dining;
import com.team8.damo.entity.enumeration.DiningStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DiningRepository extends JpaRepository<Dining, Long> {

    int countByGroupIdAndDiningStatusNot(Long groupId, DiningStatus status);

    List<Dining> findAllByGroupIdAndDiningStatus(Long groupId, DiningStatus status);

    @Modifying(clearAutomatically = true)
    @Query("update Dining d set d.attendanceVoteDoneCount = d.attendanceVoteDoneCount + 1 where d.id = :diningId")
    int increaseAttendanceVoteDoneCount(Long diningId);
}
