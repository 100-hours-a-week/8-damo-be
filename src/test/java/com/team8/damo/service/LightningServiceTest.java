package com.team8.damo.service;

import com.team8.damo.entity.Lightning;
import com.team8.damo.entity.LightningParticipant;
import com.team8.damo.entity.User;
import com.team8.damo.entity.enumeration.GatheringRole;
import com.team8.damo.entity.enumeration.LightningStatus;
import com.team8.damo.exception.CustomException;
import com.team8.damo.fixture.UserFixture;
import com.team8.damo.repository.LightningParticipantRepository;
import com.team8.damo.repository.LightningRepository;
import com.team8.damo.repository.UserRepository;
import com.team8.damo.service.request.LightningCreateServiceRequest;
import com.team8.damo.util.Snowflake;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.team8.damo.exception.errorcode.ErrorCode.LIGHTNING_DATE_MUST_BE_AFTER_NOW;
import static com.team8.damo.exception.errorcode.ErrorCode.USER_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class LightningServiceTest {

    @Mock
    private Snowflake snowflake;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LightningRepository lightningRepository;

    @Mock
    private LightningParticipantRepository participantRepository;

    @InjectMocks
    private LightningService lightningService;

    @Test
    @DisplayName("번개 모임을 성공적으로 생성한다.")
    void createLightning_success() {
        // given
        Long userId = 1L;
        Long lightningId = 100L;
        Long participantId = 200L;
        LocalDateTime currentTime = LocalDateTime.of(2025, 1, 1, 12, 0);
        LocalDateTime lightningDate = LocalDateTime.of(2025, 1, 2, 18, 0);

        LightningCreateServiceRequest request = new LightningCreateServiceRequest(
            "restaurant-1",
            4,
            "같이 밥 먹어요",
            lightningDate
        );

        User user = UserFixture.create(userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(snowflake.nextId()).willReturn(lightningId, participantId);

        // when
        Long result = lightningService.createLightning(userId, request, currentTime);

        // then
        assertThat(result).isEqualTo(lightningId);

        ArgumentCaptor<Lightning> lightningCaptor = ArgumentCaptor.forClass(Lightning.class);
        ArgumentCaptor<LightningParticipant> participantCaptor = ArgumentCaptor.forClass(LightningParticipant.class);

        then(lightningRepository).should().save(lightningCaptor.capture());
        then(participantRepository).should().save(participantCaptor.capture());

        Lightning savedLightning = lightningCaptor.getValue();
        assertThat(savedLightning)
            .extracting("id", "restaurantId", "maxParticipants", "description", "lightningDate", "lightningStatus")
            .contains(lightningId, "restaurant-1", 4, "같이 밥 먹어요", lightningDate, LightningStatus.OPEN);

        LightningParticipant savedParticipant = participantCaptor.getValue();
        assertThat(savedParticipant)
            .extracting("id", "lightning", "user", "role")
            .contains(participantId, savedLightning, user, GatheringRole.LEADER);
    }

    @Test
    @DisplayName("설명 없이 번개 모임을 생성할 수 있다.")
    void createLightning_withoutDescription() {
        // given
        Long userId = 1L;
        Long lightningId = 100L;
        Long participantId = 200L;
        LocalDateTime currentTime = LocalDateTime.of(2025, 1, 1, 12, 0);
        LocalDateTime lightningDate = LocalDateTime.of(2025, 1, 2, 18, 0);

        LightningCreateServiceRequest request = new LightningCreateServiceRequest(
            "restaurant-1",
            4,
            null,
            lightningDate
        );

        User user = UserFixture.create(userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(snowflake.nextId()).willReturn(lightningId, participantId);

        // when
        Long result = lightningService.createLightning(userId, request, currentTime);

        // then
        assertThat(result).isEqualTo(lightningId);

        ArgumentCaptor<Lightning> lightningCaptor = ArgumentCaptor.forClass(Lightning.class);
        then(lightningRepository).should().save(lightningCaptor.capture());

        Lightning savedLightning = lightningCaptor.getValue();
        assertThat(savedLightning.getDescription()).isNull();
    }

    @Test
    @DisplayName("번개 모임 날짜가 현재 시간 이전이면 생성할 수 없다.")
    void createLightning_dateBeforeNow() {
        // given
        Long userId = 1L;
        LocalDateTime currentTime = LocalDateTime.of(2025, 1, 2, 12, 0);
        LocalDateTime lightningDate = LocalDateTime.of(2025, 1, 1, 18, 0);

        LightningCreateServiceRequest request = new LightningCreateServiceRequest(
            "restaurant-1",
            4,
            "같이 밥 먹어요",
            lightningDate
        );

        // when // then
        assertThatThrownBy(() -> lightningService.createLightning(userId, request, currentTime))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", LIGHTNING_DATE_MUST_BE_AFTER_NOW);

        then(userRepository).should(never()).findById(any());
        then(lightningRepository).should(never()).save(any());
        then(participantRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 번개 모임을 생성할 수 없다.")
    void createLightning_userNotFound() {
        // given
        Long userId = 999L;
        LocalDateTime currentTime = LocalDateTime.of(2025, 1, 1, 12, 0);
        LocalDateTime lightningDate = LocalDateTime.of(2025, 1, 2, 18, 0);

        LightningCreateServiceRequest request = new LightningCreateServiceRequest(
            "restaurant-1",
            4,
            "같이 밥 먹어요",
            lightningDate
        );

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> lightningService.createLightning(userId, request, currentTime))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", USER_NOT_FOUND);

        then(userRepository).should().findById(userId);
        then(lightningRepository).should(never()).save(any());
        then(participantRepository).should(never()).save(any());
    }
}
