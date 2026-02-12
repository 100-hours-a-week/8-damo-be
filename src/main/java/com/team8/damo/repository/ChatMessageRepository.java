package com.team8.damo.repository;

import com.team8.damo.entity.ChatMessage;
import com.team8.damo.repository.projections.UnreadCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT chat.lightning.id as lightningId, COUNT(chat) as unreadCount " +
            "FROM ChatMessage chat " +
            "JOIN LightningParticipant lp ON lp.lightning.id = chat.lightning.id AND lp.user.id = :userId " +
            "WHERE chat.id > lp.lastReadChatMessageId " +
            "GROUP BY chat.lightning.id")
    List<UnreadCount> countUnreadMessagesByUser(@Param("userId") Long userId);

}
