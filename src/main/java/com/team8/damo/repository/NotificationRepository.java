package com.team8.damo.repository;

import com.team8.damo.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 커서 기반 조회: 첫 페이지 (cursor 없을 때)
    List<Notification> findAllByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    // 커서 기반 조회: cursor(id) 이후 페이지
    List<Notification> findAllByUserIdAndIdLessThanOrderByIdDesc(Long userId, Long cursorId, Pageable pageable);

    long countByUserIdAndIsReadFalse(Long userId);
}
