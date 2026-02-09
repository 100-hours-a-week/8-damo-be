package com.team8.damo.service;

import com.team8.damo.aop.CustomLock;
import com.team8.damo.entity.Lightning;
import com.team8.damo.entity.LightningParticipant;
import com.team8.damo.entity.Restaurant;
import com.team8.damo.entity.User;
import com.team8.damo.entity.enumeration.LightningStatus;
import com.team8.damo.exception.CustomException;
import com.team8.damo.repository.LightningParticipantRepository;
import com.team8.damo.repository.LightningRepository;
import com.team8.damo.repository.RestaurantRepository;
import com.team8.damo.repository.UserRepository;
import com.team8.damo.service.request.LightningCreateServiceRequest;
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

        Map<Long, Long> participantsCountMap = lightningParticipantRepository.findAllByLightningIdIn(lightningIds)
            .stream()
            .collect(Collectors.groupingBy(
                p -> p.getLightning().getId(),
                Collectors.counting()
            ));

        List<String> restaurantIds = lightningParticipants.stream()
            .map(p -> p.getLightning().getRestaurantId())
            .distinct()
            .toList();

        Map<String, String> restaurantNameMap = restaurantRepository.findAllById(restaurantIds)
            .stream()
            .collect(Collectors.toMap(
                Restaurant::getId,
                Restaurant::getPlaceName
            ));

        return lightningParticipants.stream()
            .map(p -> {
                Lightning lightning = p.getLightning();
                return LightningResponse.of(
                    lightning,
                    restaurantNameMap.getOrDefault(lightning.getRestaurantId(), ""),
                    participantsCountMap.getOrDefault(lightning.getId(), 0L).intValue(),
                    p.getRole()
                );
            })
            .toList();
    }

    private User findUserBy(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }

    private Lightning findLightningBy(Long lightningId) {
        return lightningRepository.findById(lightningId)
            .orElseThrow(() -> new CustomException(LIGHTNING_NOT_FOUND));
    }
}
