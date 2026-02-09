package com.team8.damo.service;

import com.team8.damo.entity.Lightning;
import com.team8.damo.entity.LightningParticipant;
import com.team8.damo.entity.Restaurant;
import com.team8.damo.entity.User;
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

import static com.team8.damo.exception.errorcode.ErrorCode.LIGHTNING_DATE_MUST_BE_AFTER_NOW;
import static com.team8.damo.exception.errorcode.ErrorCode.USER_NOT_FOUND;

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
    public Long createLightning(Long userId, LightningCreateServiceRequest request, LocalDateTime currentTime) {
        if (request.lightningDate().isBefore(currentTime)) {
            throw new CustomException(LIGHTNING_DATE_MUST_BE_AFTER_NOW);
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        Lightning gathering = Lightning.builder()
            .id(snowflake.nextId())
            .restaurantId(request.restaurantId())
            .maxParticipants(request.maxParticipants())
            .description(request.description())
            .lightningDate(request.lightningDate())
            .build();
        lightningRepository.save(gathering);

        LightningParticipant leader = LightningParticipant.createLeader(snowflake.nextId(), gathering, user);
        lightningParticipantRepository.save(leader);

        return gathering.getId();
    }

    public List<LightningResponse> getParticipantLightningList(Long userId, LocalDateTime currentTime, int cutoff) {
        userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

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
}
