package com.team8.damo.repository;

import com.team8.damo.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupRepository extends JpaRepository<Group, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Group g set g.totalMembers = g.totalMembers + 1 where g.id = :groupId")
    void increaseTotalMembers(@Param("groupId") Long groupId);
}
