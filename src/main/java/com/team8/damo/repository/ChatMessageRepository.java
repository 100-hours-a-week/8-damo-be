package com.team8.damo.repository;

import com.team8.damo.entity.ChatMessage;
import com.team8.damo.repository.projections.UnreadCount;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT cm FROM ChatMessage cm " +
        "JOIN FETCH cm.user " +
        "WHERE cm.lightning.id = :lightningId AND cm.id < :cursorId")
    List<ChatMessage> findPrevMessages(@Param("lightningId") Long lightningId, @Param("cursorId") Long cursorId, Pageable pageable);

    @Query("SELECT cm FROM ChatMessage cm " +
        "JOIN FETCH cm.user " +
        "WHERE cm.lightning.id = :lightningId AND cm.id <= :cursorId")
    List<ChatMessage> findPrevOrEqualMessages(@Param("lightningId") Long lightningId, @Param("cursorId") Long cursorId, Pageable pageable);

    @Query("SELECT cm FROM ChatMessage cm " +
        "JOIN FETCH cm.user " +
        "WHERE cm.lightning.id = :lightningId AND cm.id > :cursorId")
    List<ChatMessage> findNextMessages(@Param("lightningId") Long lightningId, @Param("cursorId") Long cursorId, Pageable pageable);

    @Query("SELECT MAX(cm.id) FROM ChatMessage cm WHERE cm.lightning.id = :lightningId")
    Long findLatestMessageId(@Param("lightningId") Long lightningId);
}
