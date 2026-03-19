package com.team8.damo.repository;

import com.team8.damo.entity.ReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReadStatusRepository extends JpaRepository<ReadStatus, Long> {

    @Query("select count(rs) from ReadStatus rs where rs.chatMessage.content like concat(:prefix, '%')")
    long countByChatMessageContentPrefix(@Param("prefix") String prefix);

    @Modifying
    @Query("delete from ReadStatus rs where rs.chatMessage.content like concat(:prefix, '%')")
    int deleteAllByChatMessageContentPrefix(@Param("prefix") String prefix);
}
