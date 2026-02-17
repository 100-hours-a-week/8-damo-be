package com.team8.damo.service;

import com.team8.damo.aop.CustomLock;
import com.team8.damo.entity.Lightning;
import com.team8.damo.entity.LightningParticipant;
import com.team8.damo.entity.Restaurant;
import com.team8.damo.entity.User;
import com.team8.damo.entity.enumeration.LightningStatus;
import com.team8.damo.event.EventType;
import com.team8.damo.event.handler.CommonEventPublisher;
import com.team8.damo.event.payload.UpdateUnreadCountEventPayload;
import com.team8.damo.exception.CustomException;
import com.team8.damo.repository.*;
import com.team8.damo.repository.projections.UnreadCount;
import com.team8.damo.service.request.LightningCreateServiceRequest;
import com.team8.damo.service.response.AvailableLightningResponse;
import com.team8.damo.service.response.LightningDetailResponse;
import com.team8.damo.service.response.LightningResponse;
import com.team8.damo.util.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.team8.damo.exception.errorcode.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LightningService {

    private final Snowflake snowflake;
    private final UserRepository userRepository;
    private final LightningRepository lightningRepository;
    private final LightningParticipantRepository lightningParticipantRepository;
    private final RestaurantRepository restaurantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CommonEventPublisher commonEventPublisher;

    @Transactional
    @CustomLock(key = "#lightningId")
    public Long joinLightning(Long userId, Long lightningId) {
        Lightning lightning = findLightningBy(lightningId);

        if (lightning.getLightningStatus() != LightningStatus.OPEN) {
            throw new CustomException(LIGHTNING_CLOSED);
        }

        if (lightningParticipantRepository.existsByLightningIdAndUserId(lightningId, userId)) {
            throw new CustomException(DUPLICATE_LIGHTNING_PARTICIPANT);
        }

        if (lightningParticipantRepository.countByLightningId(lightningId) >= lightning.getMaxParticipants()) {
            throw new CustomException(LIGHTNING_CAPACITY_EXCEEDED);
        }

        User user = findUserBy(userId);

        LightningParticipant participant = LightningParticipant.createParticipant(snowflake.nextId(), lightning, user);
        lightningParticipantRepository.save(participant);

        return lightningId;
    }

    @Transactional
    public Long createLightning(Long userId, LightningCreateServiceRequest request, LocalDateTime currentTime) {
        if (request.lightningDate().isBefore(currentTime)) {
            throw new CustomException(LIGHTNING_DATE_MUST_BE_AFTER_NOW);
        }

        User user = findUserBy(userId);

        Lightning lightning = Lightning.builder()
            .id(snowflake.nextId())
            .restaurantId(request.restaurantId())
            .maxParticipants(request.maxParticipants())
            .description(request.description())
            .lightningDate(request.lightningDate())
            .build();
        lightningRepository.save(lightning);

        LightningParticipant leader = LightningParticipant.createLeader(snowflake.nextId(), lightning, user);
        lightningParticipantRepository.save(leader);

        return lightning.getId();
    }

    public List<LightningResponse> getParticipantLightningList(Long userId, LocalDateTime currentTime, int cutoff) {
        findUserBy(userId);

        LocalDateTime cutoffDate = currentTime.minusDays(cutoff);

        List<LightningParticipant> lightningParticipants =
            lightningParticipantRepository.findLightningByUserIdAndCutoffDate(userId, cutoffDate);

        if (lightningParticipants.isEmpty()) {
            return List.of();
        }

        List<Long> lightningIds = lightningParticipants.stream()
            .map(p -> p.getLightning().getId())
            .toList();

        Map<Long, Long> participantsCountMap = createParticipantCountingMap(lightningIds);

        List<String> restaurantIds = lightningParticipants.stream()
            .map(p -> p.getLightning().getRestaurantId())
            .distinct()
            .toList();

        Map<String, String> restaurantNameMap = createRestaurantNameMap(restaurantIds);

        Map<Long, Integer> unreadCountMap = createUnreadCountMap(userId);

        return lightningParticipants.stream()
            .map(p -> {
                Lightning lightning = p.getLightning();
                return LightningResponse.of(
                    lightning,
                    restaurantNameMap.getOrDefault(lightning.getRestaurantId(), ""),
                    participantsCountMap.getOrDefault(lightning.getId(), 0L).intValue(),
                    unreadCountMap.getOrDefault(lightning.getId(), 0),
                    p.getRole()
                );
            })
            .toList();
    }

    @Transactional
    public void closeLightning(Long userId, Long lightningId) {
        LightningParticipant participant = lightningParticipantRepository.findByLightningIdAndUserId(lightningId, userId)
            .orElseThrow(() -> new CustomException(LIGHTNING_PARTICIPANT_NOT_FOUND));

        if (participant.isNotLeader()) {
            throw new CustomException(LIGHTNING_CLOSE_ONLY_LEADER);
        }

        Lightning lightning = participant.getLightning();

        if (lightning.isClosed()) {
            throw new CustomException(LIGHTNING_ALREADY_CLOSED);
        }

        lightning.close();
    }

    @Transactional
    @CustomLock(key = "#lightningId")
    public void leaveLightning(Long userId, Long lightningId) {
        LightningParticipant participant = lightningParticipantRepository.findByLightningIdAndUserId(lightningId, userId)
            .orElseThrow(() -> new CustomException(LIGHTNING_PARTICIPANT_NOT_FOUND));

        if (participant.isNotLeader()) {
            lightningParticipantRepository.delete(participant);
            return;
        }

        long participantCount = lightningParticipantRepository.countByLightningId(lightningId);
        if (participantCount > 1) {
            throw new CustomException(LIGHTNING_LEADER_CANNOT_LEAVE);
        }

        participant.getLightning().delete();
        lightningParticipantRepository.delete(participant);
    }

    public LightningDetailResponse getLightningDetail(Long lightningId) {
        Lightning lightning = lightningRepository.findById(lightningId)
            .filter(l -> l.getLightningStatus() != LightningStatus.DELETED)
            .orElseThrow(() -> new CustomException(LIGHTNING_NOT_FOUND));

        List<LightningParticipant> participants = lightningParticipantRepository.findAllByLightningIdWithUser(lightningId);

        Restaurant restaurant = restaurantRepository.findById(lightning.getRestaurantId()).orElse(null);

        return LightningDetailResponse.of(lightning, restaurant, participants);
    }

    public List<AvailableLightningResponse> getAvailableLightningList(Long userId) {
        findUserBy(userId);

        List<Lightning> availableLightnings = lightningRepository.findAllByStatusAndUserNotParticipating(
            LightningStatus.OPEN, userId
        );

        if (availableLightnings.isEmpty()) {
            return List.of();
        }

        List<Long> lightningIds = availableLightnings.stream()
            .map(Lightning::getId)
            .toList();

        Map<Long, Long> participantsCountMap = createParticipantCountingMap(lightningIds);

        List<String> restaurantIds = availableLightnings.stream()
            .map(Lightning::getRestaurantId)
            .distinct()
            .toList();

        Map<String, String> restaurantNameMap = createRestaurantNameMap(restaurantIds);

        return availableLightnings.stream()
            .map(lightning -> AvailableLightningResponse.of(
                lightning,
                restaurantNameMap.getOrDefault(lightning.getRestaurantId(), ""),
                participantsCountMap.getOrDefault(lightning.getId(), 0L).intValue()
            ))
            .toList();
    }

    @Transactional
    public void onSubscribe(Long userId, Long lightningId) {
        LightningParticipant participant = findParticipantBy(userId, lightningId);
//        lightningParticipantRepository.updateLastReadChatMessageId(userId, lightningId, null);

        Long startChatMessageId = participant.getLastReadChatMessageId();
        Long endChatMessageId = chatMessageRepository.findLatestMessageId(lightningId);

        participant.updateLastReadChatMessageId(null);

        commonEventPublisher.publish(
            EventType.UPDATE_UNREAD_COUNT,
            UpdateUnreadCountEventPayload.builder()
                .userId(userId)
                .lightningId(lightningId)
                .startChatMessageId(startChatMessageId)
                .endChatMessageId(endChatMessageId)
                .build()
        );
    }

    @Transactional
    public void onUnsubscribe(Long userId, Long lightningId) {
        Long latestMessageId = chatMessageRepository.findLatestMessageId(lightningId);
        lightningParticipantRepository.updateLastReadChatMessageId(userId, lightningId, latestMessageId);
    }

    private Map<String, String> createRestaurantNameMap(List<String> restaurantIds) {
        return restaurantRepository.findAllById(restaurantIds)
            .stream()
            .collect(Collectors.toMap(
                Restaurant::getId,
                Restaurant::getPlaceName
            ));
    }

    private Map<Long, Long> createParticipantCountingMap(List<Long> lightningIds) {
        return lightningParticipantRepository.findAllByLightningIdIn(lightningIds)
            .stream()
            .collect(Collectors.groupingBy(
                p -> p.getLightning().getId(),
                Collectors.counting()
            ));
    }

    private Map<Long, Integer> createUnreadCountMap(Long userId) {
        return chatMessageRepository.countUnreadMessagesByUser(userId).stream()
            .collect(Collectors.toMap(
                UnreadCount::getLightningId,
                UnreadCount::getUnreadCount
            ));
    }

    private User findUserBy(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }

    private Lightning findLightningBy(Long lightningId) {
        return lightningRepository.findById(lightningId)
            .orElseThrow(() -> new CustomException(LIGHTNING_NOT_FOUND));
    }

    private LightningParticipant findParticipantBy(Long userId, Long lightningId) {
        return lightningParticipantRepository.findByLightningIdAndUserId(lightningId, userId)
            .orElseThrow(() -> new CustomException(LIGHTNING_PARTICIPANT_NOT_FOUND));
    }
}
