package com.team8.damo.service;

import com.team8.damo.entity.Dining;
import com.team8.damo.entity.DiningParticipant;
import com.team8.damo.entity.Notification;
import com.team8.damo.entity.User;
import com.team8.damo.entity.enumeration.NotificationType;
import com.team8.damo.event.Event;
import com.team8.damo.event.EventType;
import com.team8.damo.event.handler.CommonEventPublisher;
import com.team8.damo.event.payload.*;
import com.team8.damo.fcm.FcmService;
import com.team8.damo.repository.DiningParticipantRepository;
import com.team8.damo.repository.DiningRepository;
import com.team8.damo.repository.NotificationRepository;
import com.team8.damo.util.Snowflake;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final DiningParticipantRepository diningParticipantRepository;
    private final DiningRepository diningRepository;
    private final FcmService fcmService;
    private final Snowflake snowflake;
    private final CommonEventPublisher commonEventPublisher;

    @Transactional
    public void sendDiningNotification(Long diningId, NotificationType type, String title, String body) {
        // 1. diningId로 ATTEND 상태 참여자 조회 (user fetch join)
        List<User> users = diningParticipantRepository.findAttendingParticipantsWithUser(diningId)
            .stream()
            .map(dp -> dp.getUser())
            .toList();

        if (users.isEmpty()) {
            log.info("[NotificationService] No attending participants for diningId={}", diningId);
            return;
        }

        // 2. 각 참여자에 대해 Notification 엔티티 생성 & 저장
        List<Notification> notifications = users.stream()
            .map(user -> Notification.create(snowflake.nextId(), user, type, title, body))
            .toList();
        notificationRepository.saveAll(notifications);

        // 3. FCM 토큰이 있는 사용자 필터링
        List<User> fcmEligibleUsers = users.stream()
            .filter(u -> u.isPushNotificationAllowed() && u.getFcmToken() != null)
            .toList();

        if (fcmEligibleUsers.isEmpty()) {
            return;
        }

        // 4. FcmService.sendMulticast 호출
        List<String> tokens = fcmEligibleUsers.stream()
            .map(User::getFcmToken)
            .toList();

        List<String> failedTokens = fcmService.sendMulticast(
            tokens, title, body, Map.of("diningId", String.valueOf(diningId))
        );

        // 5. 실패한 토큰 무효화
//        if (!failedTokens.isEmpty()) {
//            Map<String, User> tokenToUser = fcmEligibleUsers.stream()
//                .collect(Collectors.toMap(User::getFcmToken, u -> u));
//
//            failedTokens.forEach(token -> {
//                User user = tokenToUser.get(token);
//                if (user != null) {
//                    user.disablePushNotification();
//                    log.info("[NotificationService] Invalidated expired FCM token for userId={}", user.getId());
//                }
//            });
//        }
    }

    @Transactional
    public void sendDiningNotificationV2(Event<EventPayload> event) {
        NotificationInfo notificationInfo = createNotificationDto(event);
        if (notificationInfo == null) return;
        log.info("NotificationInfo: {}", notificationInfo);

        // 1. diningId로 ATTEND 상태 참여자 조회 (user fetch join)
        List<DiningParticipant> participants = diningParticipantRepository.findAttendingParticipantsWithUser(notificationInfo.diningId)
            .stream()
            .toList();

        // 2. 각 참여자에 대해 Notification 엔티티 생성 & 저장
        List<Notification> notifications = participants.stream()
            .map(participant -> Notification.create(
                snowflake.nextId(),
                participant.getUser(),
                notificationInfo.type, notificationInfo.title, notificationInfo.body
            ))
            .toList();
        notificationRepository.saveAll(notifications);

        // 3. FCM 토큰이 있는 사용자 필터링
        List<String> tokens = participants.stream()
            .filter(p -> p.getUser().isPushNotificationAllowed() && p.getUser().getFcmToken() != null)
            .map(p -> p.getUser().getFcmToken())
            .toList();

        commonEventPublisher.publishKafka(
            EventType.NOTIFICATION_SEND,
            NotificationEventPayload.builder()
                .tokens(tokens)
                .notificationInfo(notificationInfo)
                .build()
        );
    }

    private NotificationInfo createNotificationDto(Event<EventPayload> event) {
        Dining dining = getDining(event);

        if (dining == null) return null;

        return switch (event.getEventType()) {
            case RECOMMENDATION_REQUEST -> new NotificationInfo(
                dining.getGroup().getId(),
                dining.getId(),
                "\"" + dining.getGroup().getName() + "\"" + " 그룹의 회식 장소 추천이 시작되었어요!",
                "다모가 모두가 만족할 식당을 찾아서 알려드릴게요. 조금만 기다려주세요!",
                NotificationType.RECOMMENDATION_STARTED
            );
            case RECOMMENDATION_RESPONSE -> new NotificationInfo(
                dining.getGroup().getId(),
                dining.getId(),
                "\"" + dining.getGroup().getName() + "\"" + " 그룹의 회식 장소 추천이 완료되었어요!",
                "다모가 모두가 만족할 식당을 찾았어요. 가고 싶은 식당에 투표해보세요!",
                NotificationType.RECOMMENDATION_COMPLETED
            );
            case RESTAURANT_CONFIRMED -> new NotificationInfo(
                dining.getGroup().getId(),
                dining.getId(),
                "\"" + dining.getGroup().getName() + "\"" + "그룹의 회식 장소가 확정되었어요!",
                "그룹장이 회식 장소를 확정했어요. 회식을 진행할 장소를 확인해보세요!",
                NotificationType.RESTAURANT_CONFIRMED
            );

            default -> null;
        };
    }

    private Dining getDining(Event<EventPayload> event) {
        Long diningId = switch (event.getEventType()) {
            case RECOMMENDATION_REQUEST -> ((RecommendationV2EventPayload) event.getPayload()).diningData().diningId();
            case RECOMMENDATION_RESPONSE -> ((RecommendationDoneEventPayload) event.getPayload()).diningId();
            case RESTAURANT_CONFIRMED -> ((RestaurantConfirmedEventPayload) event.getPayload()).diningData().diningId();
            default -> null;
        };

        return diningRepository.findByIdWithGroup(diningId).orElse(null);
    }

    public record NotificationInfo(
        Long groupId,
        Long diningId,
        String title,
        String body,
        NotificationType type
    ) {
    }
}
