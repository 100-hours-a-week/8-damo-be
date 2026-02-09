package com.team8.damo.service;

import com.team8.damo.entity.Lightning;
import com.team8.damo.entity.LightningParticipant;
import com.team8.damo.entity.Restaurant;
import com.team8.damo.entity.User;
import com.team8.damo.entity.enumeration.GatheringRole;
import com.team8.damo.entity.enumeration.LightningStatus;
import com.team8.damo.exception.CustomException;
import com.team8.damo.fixture.LightningFixture;
import com.team8.damo.fixture.RestaurantFixture;
import com.team8.damo.fixture.UserFixture;
import com.team8.damo.repository.LightningParticipantRepository;
import com.team8.damo.repository.LightningRepository;
import com.team8.damo.repository.RestaurantRepository;
import com.team8.damo.repository.UserRepository;
import com.team8.damo.service.request.LightningCreateServiceRequest;
import com.team8.damo.service.response.LightningResponse;
import com.team8.damo.util.Snowflake;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.team8.damo.exception.errorcode.ErrorCode.*;
import static com.team8.damo.entity.enumeration.GatheringRole.PARTICIPANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
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

    @Mock
    private RestaurantRepository restaurantRepository;

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

    @Test
    @DisplayName("참가중인 번개 모임 목록을 성공적으로 조회한다.")
    void getParticipantLightningList_success() {
        // given
        Long userId = 1L;
        LocalDateTime currentTime = LocalDateTime.of(2025, 1, 10, 12, 0);
        int cutoff = 7;

        User user = UserFixture.create(userId);
        Lightning lightning1 = LightningFixture.create(100L, "restaurant-1", LocalDateTime.of(2025, 1, 5, 18, 0));
        Lightning lightning2 = LightningFixture.create(200L, "restaurant-2", LocalDateTime.of(2025, 1, 8, 19, 0));

        LightningParticipant participant1 = LightningParticipant.createLeader(1L, lightning1, user);
        LightningParticipant participant2 = LightningParticipant.createParticipant(2L, lightning2, user);

        User otherUser = UserFixture.create(2L);
        LightningParticipant otherParticipant = LightningParticipant.createParticipant(3L, lightning1, otherUser);

        Restaurant restaurant1 = RestaurantFixture.create("restaurant-1", "맛있는 식당");
        Restaurant restaurant2 = RestaurantFixture.create("restaurant-2", "좋은 식당");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(participantRepository.findLightningByUserIdAndCutoffDate(userId, currentTime.minusDays(cutoff)))
            .willReturn(List.of(participant1, participant2));
        given(participantRepository.findAllByLightningIdIn(List.of(100L, 200L)))
            .willReturn(List.of(participant1, participant2, otherParticipant));
        given(restaurantRepository.findAllById(List.of("restaurant-1", "restaurant-2")))
            .willReturn(List.of(restaurant1, restaurant2));

        // when
        List<LightningResponse> result = lightningService.getParticipantLightningList(userId, currentTime, cutoff);

        // then
        assertThat(result).hasSize(2)
            .extracting("lightningId", "restaurantName", "maxParticipants", "participantsCount", "lightningStatus", "myRole")
            .containsExactlyInAnyOrder(
                tuple(100L, "맛있는 식당", 4, 2, LightningStatus.OPEN, GatheringRole.LEADER),
                tuple(200L, "좋은 식당", 4, 1, LightningStatus.OPEN, GatheringRole.PARTICIPANT)
            );

        then(userRepository).should().findById(userId);
        then(participantRepository).should().findLightningByUserIdAndCutoffDate(userId, currentTime.minusDays(cutoff));
    }

    @Test
    @DisplayName("참가중인 번개 모임이 없으면 빈 리스트를 반환한다.")
    void getParticipantLightningList_emptyList() {
        // given
        Long userId = 1L;
        LocalDateTime currentTime = LocalDateTime.of(2025, 1, 10, 12, 0);
        int cutoff = 7;

        User user = UserFixture.create(userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(participantRepository.findLightningByUserIdAndCutoffDate(userId, currentTime.minusDays(cutoff)))
            .willReturn(List.of());

        // when
        List<LightningResponse> result = lightningService.getParticipantLightningList(userId, currentTime, cutoff);

        // then
        assertThat(result).isEmpty();

        then(participantRepository).should(never()).findAllByLightningIdIn(any());
        then(restaurantRepository).should(never()).findAllById(any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 번개 모임 목록을 조회할 수 없다.")
    void getParticipantLightningList_userNotFound() {
        // given
        Long userId = 999L;
        LocalDateTime currentTime = LocalDateTime.of(2025, 1, 10, 12, 0);
        int cutoff = 7;

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> lightningService.getParticipantLightningList(userId, currentTime, cutoff))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", USER_NOT_FOUND);

        then(participantRepository).should(never()).findLightningByUserIdAndCutoffDate(any(), any());
    }

    @Test
    @DisplayName("식당 정보가 없는 번개 모임은 식당명이 빈 문자열로 반환된다.")
    void getParticipantLightningList_restaurantNotFound() {
        // given
        Long userId = 1L;
        LocalDateTime currentTime = LocalDateTime.of(2025, 1, 10, 12, 0);
        int cutoff = 7;

        User user = UserFixture.create(userId);
        Lightning lightning = LightningFixture.create(100L, "unknown-restaurant");

        LightningParticipant participant = LightningParticipant.createLeader(1L, lightning, user);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(participantRepository.findLightningByUserIdAndCutoffDate(userId, currentTime.minusDays(cutoff)))
            .willReturn(List.of(participant));
        given(participantRepository.findAllByLightningIdIn(List.of(100L)))
            .willReturn(List.of(participant));
        given(restaurantRepository.findAllById(List.of("unknown-restaurant")))
            .willReturn(List.of());

        // when
        List<LightningResponse> result = lightningService.getParticipantLightningList(userId, currentTime, cutoff);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0))
            .extracting("lightningId", "restaurantName")
            .contains(100L, "");
    }

    @Test
    @DisplayName("하나의 번개 모임에 여러 참가자가 있으면 참가자 수가 정확히 집계된다.")
    void getParticipantLightningList_multipleParticipantsCount() {
        // given
        Long userId = 1L;
        LocalDateTime currentTime = LocalDateTime.of(2025, 1, 10, 12, 0);
        int cutoff = 7;

        User user = UserFixture.create(userId);
        User user2 = UserFixture.create(2L);
        User user3 = UserFixture.create(3L);
        Lightning lightning = LightningFixture.create(100L, "restaurant-1");

        LightningParticipant leader = LightningParticipant.createLeader(1L, lightning, user);
        LightningParticipant participant2 = LightningParticipant.createParticipant(2L, lightning, user2);
        LightningParticipant participant3 = LightningParticipant.createParticipant(3L, lightning, user3);

        Restaurant restaurant = RestaurantFixture.create("restaurant-1", "맛있는 식당");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(participantRepository.findLightningByUserIdAndCutoffDate(userId, currentTime.minusDays(cutoff)))
            .willReturn(List.of(leader));
        given(participantRepository.findAllByLightningIdIn(List.of(100L)))
            .willReturn(List.of(leader, participant2, participant3));
        given(restaurantRepository.findAllById(List.of("restaurant-1")))
            .willReturn(List.of(restaurant));

        // when
        List<LightningResponse> result = lightningService.getParticipantLightningList(userId, currentTime, cutoff);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0))
            .extracting("lightningId", "participantsCount", "myRole")
            .contains(100L, 3, GatheringRole.LEADER);
    }

    @Test
    @DisplayName("번개 모임에 성공적으로 참가한다.")
    void joinLightning_success() {
        // given
        Long userId = 1L;
        Long lightningId = 100L;
        Long participantId = 200L;

        Lightning lightning = LightningFixture.create(lightningId, "restaurant-1");
        User user = UserFixture.create(userId);

        given(lightningRepository.findById(lightningId)).willReturn(Optional.of(lightning));
        given(participantRepository.existsByLightningIdAndUserId(lightningId, userId)).willReturn(false);
        given(participantRepository.countByLightningId(lightningId)).willReturn(1L);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(snowflake.nextId()).willReturn(participantId);

        // when
        Long result = lightningService.joinLightning(userId, lightningId);

        // then
        assertThat(result).isEqualTo(lightningId);

        ArgumentCaptor<LightningParticipant> captor = ArgumentCaptor.forClass(LightningParticipant.class);
        then(participantRepository).should().save(captor.capture());

        LightningParticipant savedParticipant = captor.getValue();
        assertThat(savedParticipant)
            .extracting("id", "lightning", "user", "role")
            .contains(participantId, lightning, user, PARTICIPANT);

        InOrder inOrder = inOrder(lightningRepository, participantRepository, userRepository, snowflake);
        inOrder.verify(lightningRepository).findById(lightningId);
        inOrder.verify(participantRepository).existsByLightningIdAndUserId(lightningId, userId);
        inOrder.verify(participantRepository).countByLightningId(lightningId);
        inOrder.verify(userRepository).findById(userId);
        inOrder.verify(snowflake).nextId();
        inOrder.verify(participantRepository).save(any(LightningParticipant.class));
    }

    @Test
    @DisplayName("존재하지 않는 번개 모임에 참가할 수 없다.")
    void joinLightning_lightningNotFound() {
        // given
        Long userId = 1L;
        Long lightningId = 999L;

        given(lightningRepository.findById(lightningId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> lightningService.joinLightning(userId, lightningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", LIGHTNING_NOT_FOUND);

        then(participantRepository).should(never()).existsByLightningIdAndUserId(any(), any());
        then(participantRepository).should(never()).countByLightningId(any());
        then(userRepository).should(never()).findById(any());
        then(snowflake).should(never()).nextId();
        then(participantRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("모집이 마감된 번개 모임에 참가할 수 없다.")
    void joinLightning_closed() {
        // given
        Long userId = 1L;
        Long lightningId = 100L;

        Lightning lightning = LightningFixture.create(lightningId, "restaurant-1");
        ReflectionTestUtils.setField(lightning, "lightningStatus", LightningStatus.CLOSED);

        given(lightningRepository.findById(lightningId)).willReturn(Optional.of(lightning));

        // when // then
        assertThatThrownBy(() -> lightningService.joinLightning(userId, lightningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", LIGHTNING_CLOSED);

        then(participantRepository).should(never()).existsByLightningIdAndUserId(any(), any());
        then(participantRepository).should(never()).countByLightningId(any());
        then(userRepository).should(never()).findById(any());
        then(snowflake).should(never()).nextId();
        then(participantRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("이미 참가중인 번개 모임에 중복 참가할 수 없다.")
    void joinLightning_duplicateParticipant() {
        // given
        Long userId = 1L;
        Long lightningId = 100L;

        Lightning lightning = LightningFixture.create(lightningId, "restaurant-1");

        given(lightningRepository.findById(lightningId)).willReturn(Optional.of(lightning));
        given(participantRepository.existsByLightningIdAndUserId(lightningId, userId)).willReturn(true);

        // when // then
        assertThatThrownBy(() -> lightningService.joinLightning(userId, lightningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DUPLICATE_LIGHTNING_PARTICIPANT);

        then(participantRepository).should(never()).countByLightningId(any());
        then(userRepository).should(never()).findById(any());
        then(snowflake).should(never()).nextId();
        then(participantRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("정원이 가득 찬 번개 모임에 참가할 수 없다.")
    void joinLightning_capacityExceeded() {
        // given
        Long userId = 1L;
        Long lightningId = 100L;

        Lightning lightning = LightningFixture.create(lightningId, "restaurant-1", 2);

        given(lightningRepository.findById(lightningId)).willReturn(Optional.of(lightning));
        given(participantRepository.existsByLightningIdAndUserId(lightningId, userId)).willReturn(false);
        given(participantRepository.countByLightningId(lightningId)).willReturn(2L);

        // when // then
        assertThatThrownBy(() -> lightningService.joinLightning(userId, lightningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", LIGHTNING_CAPACITY_EXCEEDED);

        then(userRepository).should(never()).findById(any());
        then(snowflake).should(never()).nextId();
        then(participantRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 번개 모임에 참가할 수 없다.")
    void joinLightning_userNotFound() {
        // given
        Long userId = 999L;
        Long lightningId = 100L;

        Lightning lightning = LightningFixture.create(lightningId, "restaurant-1");

        given(lightningRepository.findById(lightningId)).willReturn(Optional.of(lightning));
        given(participantRepository.existsByLightningIdAndUserId(lightningId, userId)).willReturn(false);
        given(participantRepository.countByLightningId(lightningId)).willReturn(1L);
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> lightningService.joinLightning(userId, lightningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", USER_NOT_FOUND);

        then(snowflake).should(never()).nextId();
        then(participantRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("정원이 정확히 한 자리 남았을 때 참가할 수 있다.")
    void joinLightning_exactlyOneSpotLeft() {
        // given
        Long userId = 1L;
        Long lightningId = 100L;
        Long participantId = 200L;

        Lightning lightning = LightningFixture.create(lightningId, "restaurant-1", 3);
        User user = UserFixture.create(userId);

        given(lightningRepository.findById(lightningId)).willReturn(Optional.of(lightning));
        given(participantRepository.existsByLightningIdAndUserId(lightningId, userId)).willReturn(false);
        given(participantRepository.countByLightningId(lightningId)).willReturn(2L);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(snowflake.nextId()).willReturn(participantId);

        // when
        Long result = lightningService.joinLightning(userId, lightningId);

        // then
        assertThat(result).isEqualTo(lightningId);
        then(participantRepository).should().save(any(LightningParticipant.class));
    }

    @Nested
    @DisplayName("cutoffDate 경계값 테스트")
    class CutoffDateBoundaryTest {

        @Test
        @DisplayName("번개 날짜가 cutoffDate와 정확히 같으면 조회 결과에 포함된다.")
        void getParticipantLightningList_lightningDateEqualsCutoffDate() {
            // given
            Long userId = 1L;
            LocalDateTime currentTime = LocalDateTime.of(2025, 1, 10, 12, 0);
            int cutoff = 7;
            LocalDateTime cutoffDate = currentTime.minusDays(cutoff); // 2025-01-03 12:00

            User user = UserFixture.create(userId);
            Lightning lightning = LightningFixture.create(100L, "restaurant-1", cutoffDate);
            LightningParticipant participant = LightningParticipant.createLeader(1L, lightning, user);
            Restaurant restaurant = RestaurantFixture.create("restaurant-1", "맛있는 식당");

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(participantRepository.findLightningByUserIdAndCutoffDate(userId, cutoffDate))
                .willReturn(List.of(participant));
            given(participantRepository.findAllByLightningIdIn(List.of(100L)))
                .willReturn(List.of(participant));
            given(restaurantRepository.findAllById(List.of("restaurant-1")))
                .willReturn(List.of(restaurant));

            // when
            List<LightningResponse> result = lightningService.getParticipantLightningList(userId, currentTime, cutoff);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0))
                .extracting("lightningId", "restaurantName")
                .contains(100L, "맛있는 식당");

            then(participantRepository).should().findLightningByUserIdAndCutoffDate(userId, cutoffDate);
        }

        @Test
        @DisplayName("번개 날짜가 cutoffDate보다 이전이면 조회 결과에서 제외된다.")
        void getParticipantLightningList_lightningDateBeforeCutoffDate() {
            // given
            Long userId = 1L;
            LocalDateTime currentTime = LocalDateTime.of(2025, 1, 10, 12, 0);
            int cutoff = 7;
            LocalDateTime cutoffDate = currentTime.minusDays(cutoff); // 2025-01-03 12:00

            User user = UserFixture.create(userId);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(participantRepository.findLightningByUserIdAndCutoffDate(userId, cutoffDate))
                .willReturn(List.of());

            // when
            List<LightningResponse> result = lightningService.getParticipantLightningList(userId, currentTime, cutoff);

            // then
            assertThat(result).isEmpty();

            then(participantRepository).should().findLightningByUserIdAndCutoffDate(userId, cutoffDate);
            then(participantRepository).should(never()).findAllByLightningIdIn(any());
        }

        @Test
        @DisplayName("번개 날짜가 현재 시간 이후여도 조회 결과에 포함된다.")
        void getParticipantLightningList_lightningDateAfterCurrentTime() {
            // given
            Long userId = 1L;
            LocalDateTime currentTime = LocalDateTime.of(2025, 1, 10, 12, 0);
            int cutoff = 7;
            LocalDateTime cutoffDate = currentTime.minusDays(cutoff); // 2025-01-03 12:00
            LocalDateTime futureDate = currentTime.plusDays(1); // 2025-01-11 12:00

            User user = UserFixture.create(userId);
            Lightning lightning = LightningFixture.create(100L, "restaurant-1", futureDate);
            LightningParticipant participant = LightningParticipant.createLeader(1L, lightning, user);
            Restaurant restaurant = RestaurantFixture.create("restaurant-1", "맛있는 식당");

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(participantRepository.findLightningByUserIdAndCutoffDate(userId, cutoffDate))
                .willReturn(List.of(participant));
            given(participantRepository.findAllByLightningIdIn(List.of(100L)))
                .willReturn(List.of(participant));
            given(restaurantRepository.findAllById(List.of("restaurant-1")))
                .willReturn(List.of(restaurant));

            // when
            List<LightningResponse> result = lightningService.getParticipantLightningList(userId, currentTime, cutoff);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0))
                .extracting("lightningId", "lightningDate")
                .contains(100L, futureDate);

            then(participantRepository).should().findLightningByUserIdAndCutoffDate(userId, cutoffDate);
        }
    }
}
