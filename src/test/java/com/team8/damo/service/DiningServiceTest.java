package com.team8.damo.service;

import com.team8.damo.client.AiService;
import com.team8.damo.entity.*;
import com.team8.damo.entity.enumeration.AttendanceVoteStatus;
import com.team8.damo.entity.enumeration.DiningStatus;
import com.team8.damo.entity.enumeration.GroupRole;
import com.team8.damo.entity.enumeration.RestaurantVoteStatus;
import com.team8.damo.event.EventType;
import com.team8.damo.event.handler.CommonEventPublisher;
import com.team8.damo.event.payload.RecommendationRefreshEventPayload;
import com.team8.damo.exception.CustomException;
import com.team8.damo.fixture.*;
import com.team8.damo.repository.*;
import com.team8.damo.service.request.DiningCreateServiceRequest;
import com.team8.damo.service.request.RestaurantVoteServiceRequest;
import com.team8.damo.service.response.*;
import com.team8.damo.util.Snowflake;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.team8.damo.exception.errorcode.ErrorCode.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.never;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class DiningServiceTest {

    @Mock
    private Snowflake snowflake;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserGroupRepository userGroupRepository;

    @Mock
    private DiningRepository diningRepository;

    @Mock
    private DiningParticipantRepository diningParticipantRepository;

    @Mock
    private RecommendRestaurantRepository recommendRestaurantRepository;

    @Mock
    private RecommendRestaurantVoteRepository recommendRestaurantVoteRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CommonEventPublisher commonEventPublisher;

    @Mock
    private AiService aiService;

    @InjectMocks
    private DiningService diningService;

    @Test
    @DisplayName("그룹장이 회식을 성공적으로 생성한다.")
    void createDining_success() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long participantId1 = 300L;
        Long participantId2 = 301L;

        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 12, 0);
        LocalDateTime diningDate = LocalDateTime.of(2025, 12, 25, 18, 0);
        LocalDateTime voteDueDate = LocalDateTime.of(2025, 12, 20, 23, 59);

        DiningCreateServiceRequest request = new DiningCreateServiceRequest(diningDate, voteDueDate, 30000);

        User user = UserFixture.create(userId);
        User user2 = UserFixture.create(2L);
        Group group = GroupFixture.create(groupId, "맛집탐방대");

        UserGroup userGroup1 = UserGroup.createLeader(10L, user, group);
        UserGroup userGroup2 = UserGroup.createParticipant(11L, user2, group);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group));
        given(userGroupRepository.existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER)).willReturn(true);
        given(diningRepository.countByGroupIdAndDiningStatusNot(groupId, DiningStatus.CONFIRMED)).willReturn(0);
        given(snowflake.nextId()).willReturn(diningId, participantId1, participantId2);
        given(userGroupRepository.findAllByGroupIdWithUser(groupId)).willReturn(List.of(userGroup1, userGroup2));

        // when
        Long result = diningService.createDining(userId, groupId, request, now);

        // then
        assertThat(result).isEqualTo(diningId);

        ArgumentCaptor<Dining> diningCaptor = ArgumentCaptor.forClass(Dining.class);
        then(diningRepository).should().save(diningCaptor.capture());

        Dining savedDining = diningCaptor.getValue();
        assertThat(savedDining)
            .extracting("id", "group", "diningDate", "voteDueDate", "budget", "diningStatus")
            .contains(diningId, group, diningDate, voteDueDate, 30000, DiningStatus.ATTENDANCE_VOTING);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DiningParticipant>> participantsCaptor = ArgumentCaptor.forClass(List.class);
        then(diningParticipantRepository).should().saveAll(participantsCaptor.capture());

        List<DiningParticipant> savedParticipants = participantsCaptor.getValue();
        assertThat(savedParticipants).hasSize(2)
            .extracting("id", "user", "attendanceVoteStatus")
            .containsExactlyInAnyOrder(
                tuple(participantId1, user, AttendanceVoteStatus.PENDING),
                tuple(participantId2, user2, AttendanceVoteStatus.PENDING)
            );
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 회식을 생성할 수 없다.")
    void createDining_userNotFound() {
        // given
        Long userId = 999L;
        Long groupId = 100L;

        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 12, 0);
        LocalDateTime diningDate = LocalDateTime.of(2025, 12, 25, 18, 0);
        LocalDateTime voteDueDate = LocalDateTime.of(2025, 12, 20, 23, 59);

        DiningCreateServiceRequest request = new DiningCreateServiceRequest(diningDate, voteDueDate, 30000);

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> diningService.createDining(userId, groupId, request, now))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", USER_NOT_FOUND);

        then(userRepository).should().findById(userId);
        then(groupRepository).should(never()).findById(any());
        then(diningRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 그룹에는 회식을 생성할 수 없다.")
    void createDining_groupNotFound() {
        // given
        Long userId = 1L;
        Long groupId = 999L;

        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 12, 0);
        LocalDateTime diningDate = LocalDateTime.of(2025, 12, 25, 18, 0);
        LocalDateTime voteDueDate = LocalDateTime.of(2025, 12, 20, 23, 59);

        DiningCreateServiceRequest request = new DiningCreateServiceRequest(diningDate, voteDueDate, 30000);
        User user = UserFixture.create(userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(groupRepository.findById(groupId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> diningService.createDining(userId, groupId, request, now))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", GROUP_NOT_FOUND);

        then(userRepository).should().findById(userId);
        then(groupRepository).should().findById(groupId);
        then(diningRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("그룹장이 아닌 사용자는 회식을 생성할 수 없다.")
    void createDining_onlyGroupLeaderAllowed() {
        // given
        Long userId = 1L;
        Long groupId = 100L;

        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 12, 0);
        LocalDateTime diningDate = LocalDateTime.of(2025, 12, 25, 18, 0);
        LocalDateTime voteDueDate = LocalDateTime.of(2025, 12, 20, 23, 59);

        DiningCreateServiceRequest request = new DiningCreateServiceRequest(diningDate, voteDueDate, 30000);
        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group));
        given(userGroupRepository.existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER)).willReturn(false);

        // when // then
        assertThatThrownBy(() -> diningService.createDining(userId, groupId, request, now))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ONLY_GROUP_LEADER_ALLOWED);

        then(userGroupRepository).should().existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER);
        then(diningRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("회식 진행 날짜가 현재 날짜보다 이전이면 회식을 생성할 수 없다.")
    void createDining_diningDateMustBeAfterNow() {
        // given
        Long userId = 1L;
        Long groupId = 100L;

        LocalDateTime now = LocalDateTime.of(2025, 12, 26, 12, 0);
        LocalDateTime diningDate = LocalDateTime.of(2025, 12, 25, 18, 0);
        LocalDateTime voteDueDate = LocalDateTime.of(2025, 12, 20, 23, 59);

        DiningCreateServiceRequest request = new DiningCreateServiceRequest(diningDate, voteDueDate, 30000);
        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group));
        given(userGroupRepository.existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER)).willReturn(true);

        // when // then
        assertThatThrownBy(() -> diningService.createDining(userId, groupId, request, now))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DINING_DATE_MUST_BE_AFTER_NOW);

        then(diningRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("회식 진행 날짜와 현재 날짜가 같으면 회식을 생성할 수 없다.")
    void createDining_diningDateEqualToNow() {
        // given
        Long userId = 1L;
        Long groupId = 100L;

        LocalDateTime now = LocalDateTime.of(2025, 12, 25, 18, 0);
        LocalDateTime diningDate = LocalDateTime.of(2025, 12, 25, 18, 0);
        LocalDateTime voteDueDate = LocalDateTime.of(2025, 12, 20, 23, 59);

        DiningCreateServiceRequest request = new DiningCreateServiceRequest(diningDate, voteDueDate, 30000);
        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group));
        given(userGroupRepository.existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER)).willReturn(true);

        // when // then
        assertThatThrownBy(() -> diningService.createDining(userId, groupId, request, now))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DINING_DATE_MUST_BE_AFTER_NOW);

        then(diningRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("투표 마감 날짜가 회식 진행 날짜 이후이면 회식을 생성할 수 없다.")
    void createDining_voteDueDateMustBeBeforeDiningDate() {
        // given
        Long userId = 1L;
        Long groupId = 100L;

        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 12, 0);
        LocalDateTime diningDate = LocalDateTime.of(2025, 12, 25, 18, 0);
        LocalDateTime voteDueDate = LocalDateTime.of(2025, 12, 26, 23, 59);

        DiningCreateServiceRequest request = new DiningCreateServiceRequest(diningDate, voteDueDate, 30000);
        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group));
        given(userGroupRepository.existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER)).willReturn(true);

        // when // then
        assertThatThrownBy(() -> diningService.createDining(userId, groupId, request, now))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", VOTE_DUE_DATE_MUST_BE_BEFORE_DINING_DATE);

        then(diningRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("투표 마감 날짜와 회식 진행 날짜가 같으면 회식을 생성할 수 없다.")
    void createDining_voteDueDateEqualToDiningDate() {
        // given
        Long userId = 1L;
        Long groupId = 100L;

        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 12, 0);
        LocalDateTime diningDate = LocalDateTime.of(2025, 12, 25, 18, 0);
        LocalDateTime voteDueDate = LocalDateTime.of(2025, 12, 25, 18, 0);

        DiningCreateServiceRequest request = new DiningCreateServiceRequest(diningDate, voteDueDate, 30000);
        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group));
        given(userGroupRepository.existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER)).willReturn(true);

        // when // then
        assertThatThrownBy(() -> diningService.createDining(userId, groupId, request, now))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", VOTE_DUE_DATE_MUST_BE_BEFORE_DINING_DATE);

        then(diningRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("미완료 회식이 3개 이상이면 회식을 생성할 수 없다.")
    void createDining_diningLimitExceeded() {
        // given
        Long userId = 1L;
        Long groupId = 100L;

        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 12, 0);
        LocalDateTime diningDate = LocalDateTime.of(2025, 12, 25, 18, 0);
        LocalDateTime voteDueDate = LocalDateTime.of(2025, 12, 20, 23, 59);

        DiningCreateServiceRequest request = new DiningCreateServiceRequest(diningDate, voteDueDate, 30000);
        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group));
        given(userGroupRepository.existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER)).willReturn(true);
        given(diningRepository.countByGroupIdAndDiningStatusNot(groupId, DiningStatus.CONFIRMED)).willReturn(3);

        // when // then
        assertThatThrownBy(() -> diningService.createDining(userId, groupId, request, now))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DINING_LIMIT_EXCEEDED);

        then(diningRepository).should().countByGroupIdAndDiningStatusNot(groupId, DiningStatus.CONFIRMED);
        then(diningRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("미완료 회식이 2개이면 회식을 생성할 수 있다.")
    void createDining_withTwoIncompleteDinings() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long participantId = 300L;

        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 12, 0);
        LocalDateTime diningDate = LocalDateTime.of(2025, 12, 25, 18, 0);
        LocalDateTime voteDueDate = LocalDateTime.of(2025, 12, 20, 23, 59);

        DiningCreateServiceRequest request = new DiningCreateServiceRequest(diningDate, voteDueDate, 30000);
        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대");
        UserGroup userGroup = UserGroup.createLeader(10L, user, group);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group));
        given(userGroupRepository.existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER)).willReturn(true);
        given(diningRepository.countByGroupIdAndDiningStatusNot(groupId, DiningStatus.CONFIRMED)).willReturn(2);
        given(snowflake.nextId()).willReturn(diningId, participantId);
        given(userGroupRepository.findAllByGroupIdWithUser(groupId)).willReturn(List.of(userGroup));

        // when
        Long result = diningService.createDining(userId, groupId, request, now);

        // then
        assertThat(result).isEqualTo(diningId);
        then(diningRepository).should().save(any(Dining.class));
    }

    @Test
    @DisplayName("예산이 0원인 회식을 생성할 수 있다.")
    void createDining_withZeroBudget() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long participantId = 300L;

        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 12, 0);
        LocalDateTime diningDate = LocalDateTime.of(2025, 12, 25, 18, 0);
        LocalDateTime voteDueDate = LocalDateTime.of(2025, 12, 20, 23, 59);

        DiningCreateServiceRequest request = new DiningCreateServiceRequest(diningDate, voteDueDate, 0);
        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대");
        UserGroup userGroup = UserGroup.createLeader(10L, user, group);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group));
        given(userGroupRepository.existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER)).willReturn(true);
        given(diningRepository.countByGroupIdAndDiningStatusNot(groupId, DiningStatus.CONFIRMED)).willReturn(0);
        given(snowflake.nextId()).willReturn(diningId, participantId);
        given(userGroupRepository.findAllByGroupIdWithUser(groupId)).willReturn(List.of(userGroup));

        // when
        Long result = diningService.createDining(userId, groupId, request, now);

        // then
        assertThat(result).isEqualTo(diningId);

        ArgumentCaptor<Dining> diningCaptor = ArgumentCaptor.forClass(Dining.class);
        then(diningRepository).should().save(diningCaptor.capture());

        Dining savedDining = diningCaptor.getValue();
        assertThat(savedDining.getBudget()).isZero();
    }

    @Test
    @DisplayName("그룹에 회원이 한 명이면 한 명의 참여자만 생성된다.")
    void createDining_withSingleGroupMember() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long participantId = 300L;

        LocalDateTime now = LocalDateTime.of(2025, 1, 1, 12, 0);
        LocalDateTime diningDate = LocalDateTime.of(2025, 12, 25, 18, 0);
        LocalDateTime voteDueDate = LocalDateTime.of(2025, 12, 20, 23, 59);

        DiningCreateServiceRequest request = new DiningCreateServiceRequest(diningDate, voteDueDate, 30000);
        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대");
        UserGroup userGroup = UserGroup.createLeader(10L, user, group);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group));
        given(userGroupRepository.existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER)).willReturn(true);
        given(diningRepository.countByGroupIdAndDiningStatusNot(groupId, DiningStatus.CONFIRMED)).willReturn(0);
        given(snowflake.nextId()).willReturn(diningId, participantId);
        given(userGroupRepository.findAllByGroupIdWithUser(groupId)).willReturn(List.of(userGroup));

        // when
        Long result = diningService.createDining(userId, groupId, request, now);

        // then
        assertThat(result).isEqualTo(diningId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DiningParticipant>> participantsCaptor = ArgumentCaptor.forClass(List.class);
        then(diningParticipantRepository).should().saveAll(participantsCaptor.capture());

        List<DiningParticipant> savedParticipants = participantsCaptor.getValue();
        assertThat(savedParticipants).hasSize(1);
        assertThat(savedParticipants.get(0))
            .extracting("id", "user", "attendanceVoteStatus")
            .contains(participantId, user, AttendanceVoteStatus.PENDING);
    }

    @Test
    @DisplayName("그룹원이 회식 목록을 상태별로 조회할 수 있다.")
    void getDiningList_success() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        DiningStatus status = DiningStatus.ATTENDANCE_VOTING;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining1 = DiningFixture.create(200L, group, status);
        Dining dining2 = DiningFixture.create(201L, group, status);

        User user = UserFixture.create(userId);
        List<DiningParticipant> attendParticipants = List.of(
            DiningParticipantFixture.create(300L, dining1, user, AttendanceVoteStatus.ATTEND),
            DiningParticipantFixture.create(301L, dining1, user, AttendanceVoteStatus.ATTEND),
            DiningParticipantFixture.create(302L, dining1, user, AttendanceVoteStatus.ATTEND),
            DiningParticipantFixture.create(303L, dining2, user, AttendanceVoteStatus.ATTEND),
            DiningParticipantFixture.create(304L, dining2, user, AttendanceVoteStatus.ATTEND),
            DiningParticipantFixture.create(305L, dining2, user, AttendanceVoteStatus.ATTEND),
            DiningParticipantFixture.create(306L, dining2, user, AttendanceVoteStatus.ATTEND),
            DiningParticipantFixture.create(307L, dining2, user, AttendanceVoteStatus.ATTEND)
        );

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findAllByGroupIdAndDiningStatus(groupId, status))
            .willReturn(List.of(dining1, dining2));
        given(diningParticipantRepository.findByDiningIdInAndAttendanceVoteStatus(List.of(200L, 201L), AttendanceVoteStatus.ATTEND))
            .willReturn(attendParticipants);

        // when
        List<DiningResponse> result = diningService.getDiningList(userId, groupId, status);

        // then
        assertThat(result).hasSize(2)
            .extracting("diningId", "status", "diningParticipantsCount")
            .containsExactlyInAnyOrder(
                tuple(200L, DiningStatus.ATTENDANCE_VOTING, 3L),
                tuple(201L, DiningStatus.ATTENDANCE_VOTING, 5L)
            );

        then(userGroupRepository).should().existsByUserIdAndGroupId(userId, groupId);
        then(diningRepository).should().findAllByGroupIdAndDiningStatus(groupId, status);
    }

    @Test
    @DisplayName("그룹에 속하지 않은 사용자는 회식 목록을 조회할 수 없다.")
    void getDiningList_userNotGroupMember() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        DiningStatus status = DiningStatus.ATTENDANCE_VOTING;

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(false);

        // when // then
        assertThatThrownBy(() -> diningService.getDiningList(userId, groupId, status))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", USER_NOT_GROUP_MEMBER);

        then(userGroupRepository).should().existsByUserIdAndGroupId(userId, groupId);
        then(diningRepository).should(never()).findAllByGroupIdAndDiningStatus(any(), any());
    }

    @Test
    @DisplayName("해당 상태의 회식이 없으면 빈 목록을 반환한다.")
    void getDiningList_emptyList() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        DiningStatus status = DiningStatus.CONFIRMED;

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findAllByGroupIdAndDiningStatus(groupId, status)).willReturn(List.of());
        given(diningParticipantRepository.findByDiningIdInAndAttendanceVoteStatus(List.of(), AttendanceVoteStatus.ATTEND))
            .willReturn(List.of());

        // when
        List<DiningResponse> result = diningService.getDiningList(userId, groupId, status);

        // then
        assertThat(result).isEmpty();

        then(userGroupRepository).should().existsByUserIdAndGroupId(userId, groupId);
        then(diningRepository).should().findAllByGroupIdAndDiningStatus(groupId, status);
    }

    @Test
    @DisplayName("참석 확정 인원이 0명인 회식도 조회할 수 있다.")
    void getDiningList_withZeroParticipants() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        DiningStatus status = DiningStatus.ATTENDANCE_VOTING;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(200L, group, status);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findAllByGroupIdAndDiningStatus(groupId, status))
            .willReturn(List.of(dining));
        given(diningParticipantRepository.findByDiningIdInAndAttendanceVoteStatus(List.of(200L), AttendanceVoteStatus.ATTEND))
            .willReturn(List.of());

        // when
        List<DiningResponse> result = diningService.getDiningList(userId, groupId, status);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0))
            .extracting("diningId", "status", "diningParticipantsCount")
            .contains(200L, DiningStatus.ATTENDANCE_VOTING, 0L);
    }

    @Test
    @DisplayName("완료 상태의 회식 목록을 조회할 수 있다.")
    void getDiningList_completedStatus() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        DiningStatus status = DiningStatus.COMPLETE;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(200L, group, status);

        User user = UserFixture.create(userId);
        List<DiningParticipant> attendParticipants = List.of(
            DiningParticipantFixture.create(300L, dining, user, AttendanceVoteStatus.ATTEND),
            DiningParticipantFixture.create(301L, dining, user, AttendanceVoteStatus.ATTEND),
            DiningParticipantFixture.create(302L, dining, user, AttendanceVoteStatus.ATTEND),
            DiningParticipantFixture.create(303L, dining, user, AttendanceVoteStatus.ATTEND),
            DiningParticipantFixture.create(304L, dining, user, AttendanceVoteStatus.ATTEND),
            DiningParticipantFixture.create(305L, dining, user, AttendanceVoteStatus.ATTEND),
            DiningParticipantFixture.create(306L, dining, user, AttendanceVoteStatus.ATTEND),
            DiningParticipantFixture.create(307L, dining, user, AttendanceVoteStatus.ATTEND)
        );

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findAllByGroupIdAndDiningStatus(groupId, status))
            .willReturn(List.of(dining));
        given(diningParticipantRepository.findByDiningIdInAndAttendanceVoteStatus(List.of(200L), AttendanceVoteStatus.ATTEND))
            .willReturn(attendParticipants);

        // when
        List<DiningResponse> result = diningService.getDiningList(userId, groupId, status);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0))
            .extracting("diningId", "status", "diningParticipantsCount")
            .contains(200L, DiningStatus.COMPLETE, 8L);
    }

    @Test
    @DisplayName("장소 투표 상태의 회식 목록을 조회할 수 있다.")
    void getDiningList_restaurantVotingStatus() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        DiningStatus status = DiningStatus.RESTAURANT_VOTING;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(200L, group, status);

        User user = UserFixture.create(userId);
        List<DiningParticipant> attendParticipants = List.of(
            DiningParticipantFixture.create(300L, dining, user, AttendanceVoteStatus.ATTEND),
            DiningParticipantFixture.create(301L, dining, user, AttendanceVoteStatus.ATTEND),
            DiningParticipantFixture.create(302L, dining, user, AttendanceVoteStatus.ATTEND),
            DiningParticipantFixture.create(303L, dining, user, AttendanceVoteStatus.ATTEND),
            DiningParticipantFixture.create(304L, dining, user, AttendanceVoteStatus.ATTEND),
            DiningParticipantFixture.create(305L, dining, user, AttendanceVoteStatus.ATTEND),
            DiningParticipantFixture.create(306L, dining, user, AttendanceVoteStatus.ATTEND),
            DiningParticipantFixture.create(307L, dining, user, AttendanceVoteStatus.ATTEND)
        );

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findAllByGroupIdAndDiningStatus(groupId, status))
            .willReturn(List.of(dining));
        given(diningParticipantRepository.findByDiningIdInAndAttendanceVoteStatus(List.of(200L), AttendanceVoteStatus.ATTEND))
            .willReturn(attendParticipants);

        // when
        List<DiningResponse> result = diningService.getDiningList(userId, groupId, status);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0))
            .extracting("diningId", "status", "diningParticipantsCount")
            .contains(200L, DiningStatus.RESTAURANT_VOTING, 8L);
    }

    @Test
    @DisplayName("회식 참여자가 참석 투표를 성공적으로 한다.")
    void voteAttendance_success() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long participantId = 300L;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.ATTENDANCE_VOTING);
        User user = UserFixture.create(userId);
        DiningParticipant participant = DiningParticipantFixture.create(participantId, dining, user, AttendanceVoteStatus.PENDING);

        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(diningParticipantRepository.findByDiningIdAndUserId(diningId, userId)).willReturn(Optional.of(participant));
        given(diningRepository.getAttendanceVoteDoneCount(diningId)).willReturn(1);
        given(diningParticipantRepository.countByDiningId(diningId)).willReturn(3);

        // when
        AttendanceVoteStatus result = diningService.voteAttendance(userId, groupId, diningId, AttendanceVoteStatus.ATTEND);

        // then
        assertThat(result).isEqualTo(AttendanceVoteStatus.ATTEND);
        assertThat(participant.getAttendanceVoteStatus()).isEqualTo(AttendanceVoteStatus.ATTEND);

        then(diningRepository).should().findById(diningId);
        then(diningParticipantRepository).should().findByDiningIdAndUserId(diningId, userId);
        then(diningRepository).should().increaseAttendanceVoteDoneCount(diningId);
    }

    @Test
    @DisplayName("회식 참여자가 불참 투표를 성공적으로 한다.")
    void voteAttendance_nonAttend_success() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long participantId = 300L;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.ATTENDANCE_VOTING);
        User user = UserFixture.create(userId);
        DiningParticipant participant = DiningParticipantFixture.create(participantId, dining, user, AttendanceVoteStatus.PENDING);

        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(diningParticipantRepository.findByDiningIdAndUserId(diningId, userId)).willReturn(Optional.of(participant));
        given(diningRepository.getAttendanceVoteDoneCount(diningId)).willReturn(1);
        given(diningParticipantRepository.countByDiningId(diningId)).willReturn(3);

        // when
        AttendanceVoteStatus result = diningService.voteAttendance(userId, groupId, diningId, AttendanceVoteStatus.NON_ATTEND);

        // then
        assertThat(result).isEqualTo(AttendanceVoteStatus.NON_ATTEND);
        assertThat(participant.getAttendanceVoteStatus()).isEqualTo(AttendanceVoteStatus.NON_ATTEND);
    }

    @Test
    @DisplayName("존재하지 않는 회식에 투표할 수 없다.")
    void voteAttendance_diningNotFound() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 999L;

        given(diningRepository.findById(diningId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> diningService.voteAttendance(userId, groupId, diningId, AttendanceVoteStatus.ATTEND))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DINING_NOT_FOUND);

        then(diningRepository).should().findById(diningId);
        then(diningParticipantRepository).should(never()).findByDiningIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("참석 투표 기간이 종료된 회식에는 투표할 수 없다.")
    void voteAttendance_attendanceVotingClosed() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.RESTAURANT_VOTING);

        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));

        // when // then
        assertThatThrownBy(() -> diningService.voteAttendance(userId, groupId, diningId, AttendanceVoteStatus.ATTEND))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ATTENDANCE_VOTING_CLOSED);

        then(diningRepository).should().findById(diningId);
        then(diningParticipantRepository).should(never()).findByDiningIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("회식 확정 상태에서는 참석 투표할 수 없다.")
    void voteAttendance_confirmedStatusCannotVote() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.CONFIRMED);

        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));

        // when // then
        assertThatThrownBy(() -> diningService.voteAttendance(userId, groupId, diningId, AttendanceVoteStatus.ATTEND))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ATTENDANCE_VOTING_CLOSED);
    }

    @Test
    @DisplayName("회식 완료 상태에서는 참석 투표할 수 없다.")
    void voteAttendance_completeStatusCannotVote() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.COMPLETE);

        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));

        // when // then
        assertThatThrownBy(() -> diningService.voteAttendance(userId, groupId, diningId, AttendanceVoteStatus.ATTEND))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ATTENDANCE_VOTING_CLOSED);
    }

    @Test
    @DisplayName("회식 참여자가 아닌 사용자는 투표할 수 없다.")
    void voteAttendance_noVotePermission() {
        // given
        Long userId = 999L;
        Long groupId = 100L;
        Long diningId = 200L;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.ATTENDANCE_VOTING);

        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(diningParticipantRepository.findByDiningIdAndUserId(diningId, userId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> diningService.voteAttendance(userId, groupId, diningId, AttendanceVoteStatus.ATTEND))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DINING_PARTICIPANT_NOT_FOUND);

        then(diningRepository).should().findById(diningId);
        then(diningParticipantRepository).should().findByDiningIdAndUserId(diningId, userId);
        then(diningRepository).should(never()).increaseAttendanceVoteDoneCount(any());
    }

    @Test
    @DisplayName("참석으로 투표한 사용자가 불참으로 변경할 수 있다.")
    void voteAttendance_changeFromAttendToNonAttend_success() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long participantId = 300L;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.ATTENDANCE_VOTING);
        User user = UserFixture.create(userId);
        DiningParticipant participant = DiningParticipantFixture.create(participantId, dining, user, AttendanceVoteStatus.ATTEND);

        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(diningParticipantRepository.findByDiningIdAndUserId(diningId, userId)).willReturn(Optional.of(participant));

        // when
        AttendanceVoteStatus result = diningService.voteAttendance(userId, groupId, diningId, AttendanceVoteStatus.NON_ATTEND);

        // then
        assertThat(result).isEqualTo(AttendanceVoteStatus.NON_ATTEND);
        assertThat(participant.getAttendanceVoteStatus()).isEqualTo(AttendanceVoteStatus.NON_ATTEND);
        then(diningRepository).should(never()).increaseAttendanceVoteDoneCount(any());
    }

    @Test
    @DisplayName("불참으로 투표한 사용자가 참석으로 변경할 수 있다.")
    void voteAttendance_changeFromNonAttendToAttend_success() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long participantId = 300L;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.ATTENDANCE_VOTING);
        User user = UserFixture.create(userId);
        DiningParticipant participant = DiningParticipantFixture.create(participantId, dining, user, AttendanceVoteStatus.NON_ATTEND);

        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(diningParticipantRepository.findByDiningIdAndUserId(diningId, userId)).willReturn(Optional.of(participant));

        // when
        AttendanceVoteStatus result = diningService.voteAttendance(userId, groupId, diningId, AttendanceVoteStatus.ATTEND);

        // then
        assertThat(result).isEqualTo(AttendanceVoteStatus.ATTEND);
        assertThat(participant.getAttendanceVoteStatus()).isEqualTo(AttendanceVoteStatus.ATTEND);
        then(diningRepository).should(never()).increaseAttendanceVoteDoneCount(any());
    }

    @Test
    @DisplayName("같은 상태로 다시 투표해도 에러 없이 성공한다.")
    void voteAttendance_sameStatusAgain_success() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long participantId = 300L;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.ATTENDANCE_VOTING);
        User user = UserFixture.create(userId);
        DiningParticipant participant = DiningParticipantFixture.create(participantId, dining, user, AttendanceVoteStatus.ATTEND);

        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(diningParticipantRepository.findByDiningIdAndUserId(diningId, userId)).willReturn(Optional.of(participant));

        // when
        AttendanceVoteStatus result = diningService.voteAttendance(userId, groupId, diningId, AttendanceVoteStatus.ATTEND);

        // then
        assertThat(result).isEqualTo(AttendanceVoteStatus.ATTEND);
        then(diningRepository).should(never()).increaseAttendanceVoteDoneCount(any());
    }

    @Test
    @DisplayName("모든 참여자가 투표를 완료하면 회식 상태가 장소 투표 대기 중으로 변경된다.")
    void voteAttendance_allParticipantsVoted_changeStatusToRestaurantVoting() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long participantId = 300L;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.ATTENDANCE_VOTING);
        User user = UserFixture.create(userId);
        DiningParticipant participant = DiningParticipantFixture.create(participantId, dining, user, AttendanceVoteStatus.PENDING);

        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(diningParticipantRepository.findByDiningIdAndUserId(diningId, userId)).willReturn(Optional.of(participant));
        given(diningRepository.getAttendanceVoteDoneCount(diningId)).willReturn(3);
        given(diningParticipantRepository.countByDiningId(diningId)).willReturn(3);
        given(diningParticipantRepository.findAllByDiningAndAttendanceVoteStatus(dining, AttendanceVoteStatus.ATTEND))
            .willReturn(List.of(participant));
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group));

        // when
        AttendanceVoteStatus result = diningService.voteAttendance(userId, groupId, diningId, AttendanceVoteStatus.ATTEND);

        // then
        assertThat(result).isEqualTo(AttendanceVoteStatus.ATTEND);
        assertThat(dining.getDiningStatus()).isEqualTo(DiningStatus.RECOMMENDATION_PENDING);
    }

    @Test
    @DisplayName("아직 투표하지 않은 참여자가 있으면 회식 상태가 변경되지 않는다.")
    void voteAttendance_notAllParticipantsVoted_statusNotChanged() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long participantId = 300L;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.ATTENDANCE_VOTING);
        User user = UserFixture.create(userId);
        DiningParticipant participant = DiningParticipantFixture.create(participantId, dining, user, AttendanceVoteStatus.PENDING);

        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(diningParticipantRepository.findByDiningIdAndUserId(diningId, userId)).willReturn(Optional.of(participant));
        given(diningRepository.getAttendanceVoteDoneCount(diningId)).willReturn(2);
        given(diningParticipantRepository.countByDiningId(diningId)).willReturn(5);

        // when
        AttendanceVoteStatus result = diningService.voteAttendance(userId, groupId, diningId, AttendanceVoteStatus.ATTEND);

        // then
        assertThat(result).isEqualTo(AttendanceVoteStatus.ATTEND);
        assertThat(dining.getDiningStatus()).isEqualTo(DiningStatus.ATTENDANCE_VOTING);
    }

    @Test
    @DisplayName("1명만 있는 그룹에서 투표하면 바로 장소 투표 대기 중 상태로 변경된다.")
    void voteAttendance_singleParticipant_changeStatusImmediately() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long participantId = 300L;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.ATTENDANCE_VOTING);
        User user = UserFixture.create(userId);
        DiningParticipant participant = DiningParticipantFixture.create(participantId, dining, user, AttendanceVoteStatus.PENDING);

        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(diningParticipantRepository.findByDiningIdAndUserId(diningId, userId)).willReturn(Optional.of(participant));
        given(diningRepository.getAttendanceVoteDoneCount(diningId)).willReturn(1);
        given(diningParticipantRepository.countByDiningId(diningId)).willReturn(1);
        given(diningParticipantRepository.findAllByDiningAndAttendanceVoteStatus(dining, AttendanceVoteStatus.ATTEND))
            .willReturn(List.of(participant));
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group));

        // when
        AttendanceVoteStatus result = diningService.voteAttendance(userId, groupId, diningId, AttendanceVoteStatus.ATTEND);

        // then
        assertThat(result).isEqualTo(AttendanceVoteStatus.ATTEND);
        assertThat(dining.getDiningStatus()).isEqualTo(DiningStatus.RECOMMENDATION_PENDING);
    }

    @Test
    @DisplayName("추천 식당에 LIKE 투표를 성공적으로 한다.")
    void voteRestaurant_like_success() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long recommendRestaurantId = 400L;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.RESTAURANT_VOTING);
        User user = UserFixture.create(userId);
        RecommendRestaurant restaurant = RecommendRestaurantFixture.create(recommendRestaurantId, dining);

        RestaurantVoteServiceRequest request = new RestaurantVoteServiceRequest(RestaurantVoteStatus.LIKE);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(recommendRestaurantRepository.findById(recommendRestaurantId)).willReturn(Optional.of(restaurant));
        given(recommendRestaurantVoteRepository.findByUserIdAndRecommendRestaurantId(userId, recommendRestaurantId))
            .willReturn(Optional.empty());
        given(snowflake.nextId()).willReturn(500L);

        // when
        RestaurantVoteResponse result = diningService.voteRestaurant(userId, groupId, diningId, recommendRestaurantId, request);

        // then
        assertThat(result.recommendRestaurantId()).isEqualTo(recommendRestaurantId);
        assertThat(result.restaurantVoteStatus()).isEqualTo("LIKE");

        then(recommendRestaurantVoteRepository).should().save(any(RecommendRestaurantVote.class));
        then(recommendRestaurantRepository).should().increaseLikeCount(recommendRestaurantId);
    }

    @Test
    @DisplayName("추천 식당에 DISLIKE 투표를 성공적으로 한다.")
    void voteRestaurant_dislike_success() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long recommendRestaurantId = 400L;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.RESTAURANT_VOTING);
        User user = UserFixture.create(userId);
        RecommendRestaurant restaurant = RecommendRestaurantFixture.create(recommendRestaurantId, dining);

        RestaurantVoteServiceRequest request = new RestaurantVoteServiceRequest(RestaurantVoteStatus.DISLIKE);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(recommendRestaurantRepository.findById(recommendRestaurantId)).willReturn(Optional.of(restaurant));
        given(recommendRestaurantVoteRepository.findByUserIdAndRecommendRestaurantId(userId, recommendRestaurantId))
            .willReturn(Optional.empty());
        given(snowflake.nextId()).willReturn(500L);

        // when
        RestaurantVoteResponse result = diningService.voteRestaurant(userId, groupId, diningId, recommendRestaurantId, request);

        // then
        assertThat(result.recommendRestaurantId()).isEqualTo(recommendRestaurantId);
        assertThat(result.restaurantVoteStatus()).isEqualTo("DISLIKE");

        then(recommendRestaurantVoteRepository).should().save(any(RecommendRestaurantVote.class));
        then(recommendRestaurantRepository).should().increaseDislikeCount(recommendRestaurantId);
    }

    @Test
    @DisplayName("LIKE에서 DISLIKE로 투표를 변경한다.")
    void voteRestaurant_changeLikeToDislike() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long recommendRestaurantId = 400L;
        Long voteId = 500L;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.RESTAURANT_VOTING);
        User user = UserFixture.create(userId);
        RecommendRestaurant restaurant = RecommendRestaurantFixture.create(recommendRestaurantId, dining);
        RecommendRestaurantVote existingVote = RecommendRestaurantVoteFixture.create(voteId, user, restaurant, RestaurantVoteStatus.LIKE);

        RestaurantVoteServiceRequest request = new RestaurantVoteServiceRequest(RestaurantVoteStatus.DISLIKE);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(recommendRestaurantRepository.findById(recommendRestaurantId)).willReturn(Optional.of(restaurant));
        given(recommendRestaurantVoteRepository.findByUserIdAndRecommendRestaurantId(userId, recommendRestaurantId))
            .willReturn(Optional.of(existingVote));

        // when
        RestaurantVoteResponse result = diningService.voteRestaurant(userId, groupId, diningId, recommendRestaurantId, request);

        // then
        assertThat(result.recommendRestaurantId()).isEqualTo(recommendRestaurantId);
        assertThat(result.restaurantVoteStatus()).isEqualTo("DISLIKE");
        assertThat(existingVote.getStatus()).isEqualTo(RestaurantVoteStatus.DISLIKE);

        then(recommendRestaurantRepository).should().decreaseLikeCount(recommendRestaurantId);
        then(recommendRestaurantRepository).should().increaseDislikeCount(recommendRestaurantId);
    }

    @Test
    @DisplayName("DISLIKE에서 LIKE로 투표를 변경한다.")
    void voteRestaurant_changeDislikeToLike() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long recommendRestaurantId = 400L;
        Long voteId = 500L;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.RESTAURANT_VOTING);
        User user = UserFixture.create(userId);
        RecommendRestaurant restaurant = RecommendRestaurantFixture.create(recommendRestaurantId, dining);
        RecommendRestaurantVote existingVote = RecommendRestaurantVoteFixture.create(voteId, user, restaurant, RestaurantVoteStatus.DISLIKE);

        RestaurantVoteServiceRequest request = new RestaurantVoteServiceRequest(RestaurantVoteStatus.LIKE);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(recommendRestaurantRepository.findById(recommendRestaurantId)).willReturn(Optional.of(restaurant));
        given(recommendRestaurantVoteRepository.findByUserIdAndRecommendRestaurantId(userId, recommendRestaurantId))
            .willReturn(Optional.of(existingVote));

        // when
        RestaurantVoteResponse result = diningService.voteRestaurant(userId, groupId, diningId, recommendRestaurantId, request);

        // then
        assertThat(result.recommendRestaurantId()).isEqualTo(recommendRestaurantId);
        assertThat(result.restaurantVoteStatus()).isEqualTo("LIKE");
        assertThat(existingVote.getStatus()).isEqualTo(RestaurantVoteStatus.LIKE);

        then(recommendRestaurantRepository).should().decreaseDislikeCount(recommendRestaurantId);
        then(recommendRestaurantRepository).should().increaseLikeCount(recommendRestaurantId);
    }

    @Test
    @DisplayName("동일한 LIKE 투표를 하면 투표가 삭제된다.")
    void voteRestaurant_sameLikeVote_deleteVote() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long recommendRestaurantId = 400L;
        Long voteId = 500L;
        RestaurantVoteStatus restaurantVoteStatus = RestaurantVoteStatus.LIKE;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.RESTAURANT_VOTING);
        User user = UserFixture.create(userId);
        RecommendRestaurant restaurant = RecommendRestaurantFixture.create(recommendRestaurantId, dining);
        RecommendRestaurantVote existingVote = RecommendRestaurantVoteFixture.create(voteId, user, restaurant, restaurantVoteStatus);

        RestaurantVoteServiceRequest request = new RestaurantVoteServiceRequest(restaurantVoteStatus);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(recommendRestaurantRepository.findById(recommendRestaurantId)).willReturn(Optional.of(restaurant));
        given(recommendRestaurantVoteRepository.findByUserIdAndRecommendRestaurantId(userId, recommendRestaurantId))
            .willReturn(Optional.of(existingVote));

        // when
        RestaurantVoteResponse result = diningService.voteRestaurant(userId, groupId, diningId, recommendRestaurantId, request);

        // then
        assertThat(result.recommendRestaurantId()).isEqualTo(recommendRestaurantId);
        assertThat(result.restaurantVoteStatus()).isEqualTo("NONE");

        then(recommendRestaurantVoteRepository).should().delete(existingVote);
        then(recommendRestaurantRepository).should().decreaseLikeCount(recommendRestaurantId);
        then(recommendRestaurantRepository).should(never()).increaseLikeCount(any());
    }

    @Test
    @DisplayName("동일한 DISLIKE 투표를 하면 투표가 삭제된다.")
    void voteRestaurant_sameDislikeVote_deleteVote() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long recommendRestaurantId = 400L;
        Long voteId = 500L;
        RestaurantVoteStatus restaurantVoteStatus = RestaurantVoteStatus.DISLIKE;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.RESTAURANT_VOTING);
        User user = UserFixture.create(userId);
        RecommendRestaurant restaurant = RecommendRestaurantFixture.create(recommendRestaurantId, dining);
        RecommendRestaurantVote existingVote = RecommendRestaurantVoteFixture.create(voteId, user, restaurant, restaurantVoteStatus);

        RestaurantVoteServiceRequest request = new RestaurantVoteServiceRequest(restaurantVoteStatus);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(recommendRestaurantRepository.findById(recommendRestaurantId)).willReturn(Optional.of(restaurant));
        given(recommendRestaurantVoteRepository.findByUserIdAndRecommendRestaurantId(userId, recommendRestaurantId))
            .willReturn(Optional.of(existingVote));

        // when
        RestaurantVoteResponse result = diningService.voteRestaurant(userId, groupId, diningId, recommendRestaurantId, request);

        // then
        assertThat(result.recommendRestaurantId()).isEqualTo(recommendRestaurantId);
        assertThat(result.restaurantVoteStatus()).isEqualTo("NONE");

        then(recommendRestaurantVoteRepository).should().delete(existingVote);
        then(recommendRestaurantRepository).should().decreaseDislikeCount(recommendRestaurantId);
        then(recommendRestaurantRepository).should(never()).increaseDislikeCount(any());
    }

    @Test
    @DisplayName("그룹 멤버가 아닌 사용자는 추천 식당에 투표할 수 없다.")
    void voteRestaurant_userNotGroupMember() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long recommendRestaurantId = 400L;

        RestaurantVoteServiceRequest request = new RestaurantVoteServiceRequest(RestaurantVoteStatus.LIKE);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(false);

        // when // then
        assertThatThrownBy(() -> diningService.voteRestaurant(userId, groupId, diningId, recommendRestaurantId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", USER_NOT_GROUP_MEMBER);

        then(diningRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("식당 투표 기간이 아닌 회식에는 투표할 수 없다.")
    void voteRestaurant_restaurantVotingClosed() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long recommendRestaurantId = 400L;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.CONFIRMED);

        RestaurantVoteServiceRequest request = new RestaurantVoteServiceRequest(RestaurantVoteStatus.LIKE);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));

        // when // then
        assertThatThrownBy(() -> diningService.voteRestaurant(userId, groupId, diningId, recommendRestaurantId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", RESTAURANT_VOTING_CLOSED);

        then(userRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 회식에는 식당 투표를 할 수 없다.")
    void voteRestaurant_diningNotFound() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 999L;
        Long recommendRestaurantId = 400L;

        RestaurantVoteServiceRequest request = new RestaurantVoteServiceRequest(RestaurantVoteStatus.LIKE);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> diningService.voteRestaurant(userId, groupId, diningId, recommendRestaurantId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DINING_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 추천 식당에는 투표할 수 없다.")
    void voteRestaurant_recommendRestaurantNotFound() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long recommendRestaurantId = 999L;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.RESTAURANT_VOTING);
        User user = UserFixture.create(userId);

        RestaurantVoteServiceRequest request = new RestaurantVoteServiceRequest(RestaurantVoteStatus.LIKE);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(recommendRestaurantRepository.findById(recommendRestaurantId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> diningService.voteRestaurant(userId, groupId, diningId, recommendRestaurantId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", RECOMMEND_RESTAURANT_NOT_FOUND);
    }

    @Test
    @DisplayName("참석 투표 중 상태에서는 식당 투표를 할 수 없다.")
    void voteRestaurant_attendanceVotingStatus_cannotVote() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long recommendRestaurantId = 400L;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.ATTENDANCE_VOTING);

        RestaurantVoteServiceRequest request = new RestaurantVoteServiceRequest(RestaurantVoteStatus.LIKE);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));

        // when // then
        assertThatThrownBy(() -> diningService.voteRestaurant(userId, groupId, diningId, recommendRestaurantId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", RESTAURANT_VOTING_CLOSED);
    }

    @Test
    @DisplayName("회식 완료 상태에서는 식당 투표를 할 수 없다.")
    void voteRestaurant_completeStatus_cannotVote() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long recommendRestaurantId = 400L;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.COMPLETE);

        RestaurantVoteServiceRequest request = new RestaurantVoteServiceRequest(RestaurantVoteStatus.LIKE);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));

        // when // then
        assertThatThrownBy(() -> diningService.voteRestaurant(userId, groupId, diningId, recommendRestaurantId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", RESTAURANT_VOTING_CLOSED);
    }

    @Test
    @DisplayName("그룹장이 회식 상세를 조회한다.")
    void getDiningDetail_success_asGroupLeader() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;

        User user = UserFixture.create(userId);
        User user2 = UserFixture.create(2L);
        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.RESTAURANT_VOTING);
        UserGroup userGroup = UserGroup.createLeader(10L, user, group);

        List<DiningParticipant> attendParticipants = List.of(
            DiningParticipantFixture.create(300L, dining, user, AttendanceVoteStatus.ATTEND),
            DiningParticipantFixture.create(301L, dining, user2, AttendanceVoteStatus.ATTEND)
        );

        given(userGroupRepository.findByUserIdAndGroupId(userId, groupId))
            .willReturn(Optional.of(userGroup));
        given(diningRepository.findById(diningId))
            .willReturn(Optional.of(dining));
        given(diningParticipantRepository.findAllByDiningAndAttendanceVoteStatus(dining, AttendanceVoteStatus.ATTEND))
            .willReturn(attendParticipants);

        // when
        DiningDetailResponse result = diningService.getDiningDetail(userId, groupId, diningId);

        // then
        assertThat(result.isGroupLeader()).isTrue();
        assertThat(result.diningStatus()).isEqualTo(DiningStatus.RESTAURANT_VOTING);
        assertThat(result.diningParticipants()).hasSize(2)
            .extracting("userId")
            .containsExactlyInAnyOrder(userId, 2L);

        then(userGroupRepository).should().findByUserIdAndGroupId(userId, groupId);
        then(diningRepository).should().findById(diningId);
        then(diningParticipantRepository).should().findAllByDiningAndAttendanceVoteStatus(dining, AttendanceVoteStatus.ATTEND);
    }

    @Test
    @DisplayName("일반 그룹원이 회식 상세를 조회한다.")
    void getDiningDetail_success_asGroupMember() {
        // given
        Long userId = 2L;
        Long groupId = 100L;
        Long diningId = 200L;

        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.ATTENDANCE_VOTING);
        UserGroup userGroup = UserGroup.createParticipant(11L, user, group);

        given(userGroupRepository.findByUserIdAndGroupId(userId, groupId))
            .willReturn(Optional.of(userGroup));
        given(diningRepository.findById(diningId))
            .willReturn(Optional.of(dining));
        given(diningParticipantRepository.findAllByDiningAndAttendanceVoteStatus(dining, AttendanceVoteStatus.ATTEND))
            .willReturn(List.of());

        // when
        DiningDetailResponse result = diningService.getDiningDetail(userId, groupId, diningId);

        // then
        assertThat(result.isGroupLeader()).isFalse();
        assertThat(result.diningStatus()).isEqualTo(DiningStatus.ATTENDANCE_VOTING);
        assertThat(result.diningParticipants()).isEmpty();
    }

    @Test
    @DisplayName("참석자가 없는 회식 상세를 조회한다.")
    void getDiningDetail_withNoAttendParticipants() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;

        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.ATTENDANCE_VOTING);
        UserGroup userGroup = UserGroup.createLeader(10L, user, group);

        given(userGroupRepository.findByUserIdAndGroupId(userId, groupId))
            .willReturn(Optional.of(userGroup));
        given(diningRepository.findById(diningId))
            .willReturn(Optional.of(dining));
        given(diningParticipantRepository.findAllByDiningAndAttendanceVoteStatus(dining, AttendanceVoteStatus.ATTEND))
            .willReturn(List.of());

        // when
        DiningDetailResponse result = diningService.getDiningDetail(userId, groupId, diningId);

        // then
        assertThat(result.diningParticipants()).isEmpty();
    }

    @Test
    @DisplayName("여러 참석자가 있는 회식 상세를 조회한다.")
    void getDiningDetail_withMultipleAttendParticipants() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;

        User user1 = UserFixture.create(userId);
        User user2 = UserFixture.create(2L);
        User user3 = UserFixture.create(3L);
        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.CONFIRMED);
        UserGroup userGroup = UserGroup.createLeader(10L, user1, group);

        List<DiningParticipant> attendParticipants = List.of(
            DiningParticipantFixture.create(300L, dining, user1, AttendanceVoteStatus.ATTEND),
            DiningParticipantFixture.create(301L, dining, user2, AttendanceVoteStatus.ATTEND),
            DiningParticipantFixture.create(302L, dining, user3, AttendanceVoteStatus.ATTEND)
        );

        given(userGroupRepository.findByUserIdAndGroupId(userId, groupId))
            .willReturn(Optional.of(userGroup));
        given(diningRepository.findById(diningId))
            .willReturn(Optional.of(dining));
        given(diningParticipantRepository.findAllByDiningAndAttendanceVoteStatus(dining, AttendanceVoteStatus.ATTEND))
            .willReturn(attendParticipants);

        // when
        DiningDetailResponse result = diningService.getDiningDetail(userId, groupId, diningId);

        // then
        assertThat(result.diningParticipants()).hasSize(3)
            .extracting("userId")
            .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    @DisplayName("그룹에 속하지 않은 사용자는 회식 상세를 조회할 수 없다.")
    void getDiningDetail_userNotGroupMember() {
        // given
        Long userId = 999L;
        Long groupId = 100L;
        Long diningId = 200L;

        given(userGroupRepository.findByUserIdAndGroupId(userId, groupId))
            .willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> diningService.getDiningDetail(userId, groupId, diningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", USER_NOT_GROUP_MEMBER);

        then(userGroupRepository).should().findByUserIdAndGroupId(userId, groupId);
        then(diningRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 회식은 조회할 수 없다.")
    void getDiningDetail_diningNotFound() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 999L;

        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대");
        UserGroup userGroup = UserGroup.createLeader(10L, user, group);

        given(userGroupRepository.findByUserIdAndGroupId(userId, groupId))
            .willReturn(Optional.of(userGroup));
        given(diningRepository.findById(diningId))
            .willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> diningService.getDiningDetail(userId, groupId, diningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DINING_NOT_FOUND);

        then(userGroupRepository).should().findByUserIdAndGroupId(userId, groupId);
        then(diningRepository).should().findById(diningId);
        then(diningParticipantRepository).should(never()).findAllByDiningAndAttendanceVoteStatus(any(), any());
    }

    @Test
    @DisplayName("회식 참석/불참석 투표 현황을 조회한다.")
    void getAttendanceVoteDetail_success() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;

        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.ATTENDANCE_VOTING);
        DiningParticipant participant = DiningParticipantFixture.create(300L, dining, user, AttendanceVoteStatus.PENDING);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(diningParticipantRepository.findByDiningIdAndUserId(diningId, userId)).willReturn(Optional.of(participant));

        // when
        AttendanceVoteDetailResponse result = diningService.getAttendanceVoteDetail(userId, groupId, diningId);

        // then
        assertThat(result)
            .extracting("attendanceVoteStatus", "completedVoteCount", "totalGroupMemberCount")
            .contains(AttendanceVoteStatus.PENDING, 0, 1);

        then(userGroupRepository).should().existsByUserIdAndGroupId(userId, groupId);
        then(diningRepository).should().findById(diningId);
        then(diningParticipantRepository).should().findByDiningIdAndUserId(diningId, userId);
    }

    @Test
    @DisplayName("참석 투표를 완료한 사용자의 투표 현황을 조회한다.")
    void getAttendanceVoteDetail_withAttendVote() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;

        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.ATTENDANCE_VOTING);
        DiningParticipant participant = DiningParticipantFixture.create(300L, dining, user, AttendanceVoteStatus.ATTEND);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(diningParticipantRepository.findByDiningIdAndUserId(diningId, userId)).willReturn(Optional.of(participant));

        // when
        AttendanceVoteDetailResponse result = diningService.getAttendanceVoteDetail(userId, groupId, diningId);

        // then
        assertThat(result.attendanceVoteStatus()).isEqualTo(AttendanceVoteStatus.ATTEND);
    }

    @Test
    @DisplayName("불참 투표를 완료한 사용자의 투표 현황을 조회한다.")
    void getAttendanceVoteDetail_withNonAttendVote() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;

        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.ATTENDANCE_VOTING);
        DiningParticipant participant = DiningParticipantFixture.create(300L, dining, user, AttendanceVoteStatus.NON_ATTEND);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(diningParticipantRepository.findByDiningIdAndUserId(diningId, userId)).willReturn(Optional.of(participant));

        // when
        AttendanceVoteDetailResponse result = diningService.getAttendanceVoteDetail(userId, groupId, diningId);

        // then
        assertThat(result.attendanceVoteStatus()).isEqualTo(AttendanceVoteStatus.NON_ATTEND);
    }

    @Test
    @DisplayName("그룹에 속하지 않은 사용자는 투표 현황을 조회할 수 없다.")
    void getAttendanceVoteDetail_userNotGroupMember() {
        // given
        Long userId = 999L;
        Long groupId = 100L;
        Long diningId = 200L;

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(false);

        // when // then
        assertThatThrownBy(() -> diningService.getAttendanceVoteDetail(userId, groupId, diningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", USER_NOT_GROUP_MEMBER);

        then(userGroupRepository).should().existsByUserIdAndGroupId(userId, groupId);
        then(diningRepository).should(never()).findById(any());
        then(diningParticipantRepository).should(never()).findByDiningIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 회식의 투표 현황은 조회할 수 없다.")
    void getAttendanceVoteDetail_diningNotFound() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 999L;

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> diningService.getAttendanceVoteDetail(userId, groupId, diningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DINING_NOT_FOUND);

        then(userGroupRepository).should().existsByUserIdAndGroupId(userId, groupId);
        then(diningRepository).should().findById(diningId);
        then(diningParticipantRepository).should(never()).findByDiningIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("회식 참여자가 아닌 사용자는 투표 현황을 조회할 수 없다.")
    void getAttendanceVoteDetail_noVotePermission() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.ATTENDANCE_VOTING);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(diningParticipantRepository.findByDiningIdAndUserId(diningId, userId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> diningService.getAttendanceVoteDetail(userId, groupId, diningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DINING_PARTICIPANT_NOT_FOUND);

        then(userGroupRepository).should().existsByUserIdAndGroupId(userId, groupId);
        then(diningRepository).should().findById(diningId);
        then(diningParticipantRepository).should().findByDiningIdAndUserId(diningId, userId);
    }

    @Test
    @DisplayName("식당 투표 중 상태의 회식 투표 현황을 조회할 수 있다.")
    void getAttendanceVoteDetail_restaurantVotingStatus() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;

        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.RESTAURANT_VOTING);
        DiningParticipant participant = DiningParticipantFixture.create(300L, dining, user, AttendanceVoteStatus.ATTEND);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(diningParticipantRepository.findByDiningIdAndUserId(diningId, userId)).willReturn(Optional.of(participant));

        // when
        AttendanceVoteDetailResponse result = diningService.getAttendanceVoteDetail(userId, groupId, diningId);

        // then
        assertThat(result.attendanceVoteStatus()).isEqualTo(AttendanceVoteStatus.ATTEND);
    }

    @Test
    @DisplayName("확정 상태의 회식 투표 현황을 조회할 수 있다.")
    void getAttendanceVoteDetail_confirmedStatus() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;

        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.CONFIRMED);
        DiningParticipant participant = DiningParticipantFixture.create(300L, dining, user, AttendanceVoteStatus.ATTEND);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(diningParticipantRepository.findByDiningIdAndUserId(diningId, userId)).willReturn(Optional.of(participant));

        // when
        AttendanceVoteDetailResponse result = diningService.getAttendanceVoteDetail(userId, groupId, diningId);

        // then
        assertThat(result.attendanceVoteStatus()).isEqualTo(AttendanceVoteStatus.ATTEND);
    }

    @Test
    @DisplayName("참석 상태인 사용자가 장소 투표 현황을 조회한다.")
    void getRestaurantVoteDetail_success() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long recommendRestaurantId1 = 400L;
        Long recommendRestaurantId2 = 401L;
        String restaurantId1 = "restaurant-001";
        String restaurantId2 = "restaurant-002";
        Integer recommendationCount = 1;

        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.createWithRecommendationCount(diningId, group, DiningStatus.RESTAURANT_VOTING, recommendationCount);
        DiningParticipant participant = DiningParticipantFixture.create(300L, dining, user, AttendanceVoteStatus.ATTEND);

        RecommendRestaurant recommendRestaurant1 = RecommendRestaurantFixture.create(
            recommendRestaurantId1, dining, restaurantId1, recommendationCount, 3, 1
        );
        RecommendRestaurant recommendRestaurant2 = RecommendRestaurantFixture.create(
            recommendRestaurantId2, dining, restaurantId2, recommendationCount, 2, 2
        );

        Restaurant restaurant1 = RestaurantFixture.create(restaurantId1, "맛있는 고기집");
        Restaurant restaurant2 = RestaurantFixture.create(restaurantId2, "해산물 레스토랑");

        RecommendRestaurantVote vote1 = RecommendRestaurantVoteFixture.create(500L, user, recommendRestaurant1, RestaurantVoteStatus.LIKE);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(recommendRestaurantRepository.findByDiningIdAndRecommendationCount(diningId, recommendationCount))
            .willReturn(List.of(recommendRestaurant1, recommendRestaurant2));
        given(diningParticipantRepository.findByDiningIdAndUserId(diningId, userId)).willReturn(Optional.of(participant));
        given(restaurantRepository.findById(restaurantId1)).willReturn(Optional.of(restaurant1));
        given(restaurantRepository.findById(restaurantId2)).willReturn(Optional.of(restaurant2));
        given(recommendRestaurantVoteRepository.findByUserIdAndRecommendRestaurantId(userId, recommendRestaurantId1))
            .willReturn(Optional.of(vote1));
        given(recommendRestaurantVoteRepository.findByUserIdAndRecommendRestaurantId(userId, recommendRestaurantId2))
            .willReturn(Optional.empty());

        // when
        List<RestaurantVoteDetailResponse> result = diningService.getRestaurantVoteDetail(userId, groupId, diningId);

        // then
        assertThat(result).hasSize(2)
            .extracting("recommendRestaurantsId", "restaurantsName", "restaurantVoteStatus", "likeCount", "dislikeCount")
            .containsExactlyInAnyOrder(
                tuple(recommendRestaurantId1, "맛있는 고기집", "LIKE", 3, 1),
                tuple(recommendRestaurantId2, "해산물 레스토랑", "NONE", 2, 2)
            );

        then(userGroupRepository).should().existsByUserIdAndGroupId(userId, groupId);
        then(diningRepository).should().findById(diningId);
        then(recommendRestaurantRepository).should().findByDiningIdAndRecommendationCount(diningId, recommendationCount);
        then(diningParticipantRepository).should().findByDiningIdAndUserId(diningId, userId);
    }

    @Test
    @DisplayName("사용자가 DISLIKE로 투표한 장소의 투표 현황을 조회한다.")
    void getRestaurantVoteDetail_withDislikeVote() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long recommendRestaurantId = 400L;
        String restaurantId = "restaurant-001";
        Integer recommendationCount = 1;

        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.createWithRecommendationCount(diningId, group, DiningStatus.RESTAURANT_VOTING, recommendationCount);
        DiningParticipant participant = DiningParticipantFixture.create(300L, dining, user, AttendanceVoteStatus.ATTEND);

        RecommendRestaurant recommendRestaurant = RecommendRestaurantFixture.create(
            recommendRestaurantId, dining, restaurantId, recommendationCount, 1, 5
        );

        Restaurant restaurant = RestaurantFixture.create(restaurantId, "별로인 식당");

        RecommendRestaurantVote vote = RecommendRestaurantVoteFixture.create(500L, user, recommendRestaurant, RestaurantVoteStatus.DISLIKE);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(recommendRestaurantRepository.findByDiningIdAndRecommendationCount(diningId, recommendationCount))
            .willReturn(List.of(recommendRestaurant));
        given(diningParticipantRepository.findByDiningIdAndUserId(diningId, userId)).willReturn(Optional.of(participant));
        given(restaurantRepository.findById(restaurantId)).willReturn(Optional.of(restaurant));
        given(recommendRestaurantVoteRepository.findByUserIdAndRecommendRestaurantId(userId, recommendRestaurantId))
            .willReturn(Optional.of(vote));

        // when
        List<RestaurantVoteDetailResponse> result = diningService.getRestaurantVoteDetail(userId, groupId, diningId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0))
            .extracting("recommendRestaurantsId", "restaurantsName", "restaurantVoteStatus", "likeCount", "dislikeCount")
            .contains(recommendRestaurantId, "별로인 식당", "DISLIKE", 1, 5);
    }

    @Test
    @DisplayName("투표하지 않은 사용자가 장소 투표 현황을 조회하면 NONE으로 표시된다.")
    void getRestaurantVoteDetail_withNoVote() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long recommendRestaurantId = 400L;
        String restaurantId = "restaurant-001";
        Integer recommendationCount = 1;

        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.createWithRecommendationCount(diningId, group, DiningStatus.RESTAURANT_VOTING, recommendationCount);
        DiningParticipant participant = DiningParticipantFixture.create(300L, dining, user, AttendanceVoteStatus.ATTEND);

        RecommendRestaurant recommendRestaurant = RecommendRestaurantFixture.create(
            recommendRestaurantId, dining, restaurantId, recommendationCount
        );

        Restaurant restaurant = RestaurantFixture.create(restaurantId);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(recommendRestaurantRepository.findByDiningIdAndRecommendationCount(diningId, recommendationCount))
            .willReturn(List.of(recommendRestaurant));
        given(diningParticipantRepository.findByDiningIdAndUserId(diningId, userId)).willReturn(Optional.of(participant));
        given(restaurantRepository.findById(restaurantId)).willReturn(Optional.of(restaurant));
        given(recommendRestaurantVoteRepository.findByUserIdAndRecommendRestaurantId(userId, recommendRestaurantId))
            .willReturn(Optional.empty());

        // when
        List<RestaurantVoteDetailResponse> result = diningService.getRestaurantVoteDetail(userId, groupId, diningId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).restaurantVoteStatus()).isEqualTo("NONE");
    }

    @Test
    @DisplayName("추천 장소가 없으면 빈 목록을 반환한다.")
    void getRestaurantVoteDetail_emptyRecommendRestaurants() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Integer recommendationCount = 1;

        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.createWithRecommendationCount(diningId, group, DiningStatus.RESTAURANT_VOTING, recommendationCount);
        DiningParticipant participant = DiningParticipantFixture.create(300L, dining, user, AttendanceVoteStatus.ATTEND);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(recommendRestaurantRepository.findByDiningIdAndRecommendationCount(diningId, recommendationCount))
            .willReturn(List.of());
        given(diningParticipantRepository.findByDiningIdAndUserId(diningId, userId)).willReturn(Optional.of(participant));

        // when
        List<RestaurantVoteDetailResponse> result = diningService.getRestaurantVoteDetail(userId, groupId, diningId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("그룹에 속하지 않은 사용자는 장소 투표 현황을 조회할 수 없다.")
    void getRestaurantVoteDetail_userNotGroupMember() {
        // given
        Long userId = 999L;
        Long groupId = 100L;
        Long diningId = 200L;

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(false);

        // when // then
        assertThatThrownBy(() -> diningService.getRestaurantVoteDetail(userId, groupId, diningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", USER_NOT_GROUP_MEMBER);

        then(userGroupRepository).should().existsByUserIdAndGroupId(userId, groupId);
        then(diningRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 회식의 장소 투표 현황은 조회할 수 없다.")
    void getRestaurantVoteDetail_diningNotFound() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 999L;

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> diningService.getRestaurantVoteDetail(userId, groupId, diningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DINING_NOT_FOUND);

        then(userGroupRepository).should().existsByUserIdAndGroupId(userId, groupId);
        then(diningRepository).should().findById(diningId);
        then(recommendRestaurantRepository).should(never()).findByDiningIdAndRecommendationCount(any(), any());
    }

    @Test
    @DisplayName("회식 참여자가 아닌 사용자는 장소 투표 현황을 조회할 수 없다.")
    void getRestaurantVoteDetail_participantNotFound() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Integer recommendationCount = 1;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.createWithRecommendationCount(diningId, group, DiningStatus.RESTAURANT_VOTING, recommendationCount);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(recommendRestaurantRepository.findByDiningIdAndRecommendationCount(diningId, recommendationCount))
            .willReturn(List.of());
        given(diningParticipantRepository.findByDiningIdAndUserId(diningId, userId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> diningService.getRestaurantVoteDetail(userId, groupId, diningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DINING_PARTICIPANT_NOT_FOUND);

        then(diningParticipantRepository).should().findByDiningIdAndUserId(diningId, userId);
    }

    @Test
    @DisplayName("불참 상태인 사용자는 장소 투표 현황을 조회할 수 없다.")
    void getRestaurantVoteDetail_nonAttendParticipantCannotView() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Integer recommendationCount = 1;

        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.createWithRecommendationCount(diningId, group, DiningStatus.RESTAURANT_VOTING, recommendationCount);
        DiningParticipant participant = DiningParticipantFixture.create(300L, dining, user, AttendanceVoteStatus.NON_ATTEND);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(recommendRestaurantRepository.findByDiningIdAndRecommendationCount(diningId, recommendationCount))
            .willReturn(List.of());
        given(diningParticipantRepository.findByDiningIdAndUserId(diningId, userId)).willReturn(Optional.of(participant));

        // when // then
        assertThatThrownBy(() -> diningService.getRestaurantVoteDetail(userId, groupId, diningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ONLY_ATTEND_PARTICIPANT_CAN_VOTE);
    }

    @Test
    @DisplayName("참석 투표 대기 중인 사용자는 장소 투표 현황을 조회할 수 없다.")
    void getRestaurantVoteDetail_pendingParticipantCannotView() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Integer recommendationCount = 1;

        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.createWithRecommendationCount(diningId, group, DiningStatus.RESTAURANT_VOTING, recommendationCount);
        DiningParticipant participant = DiningParticipantFixture.create(300L, dining, user, AttendanceVoteStatus.PENDING);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(recommendRestaurantRepository.findByDiningIdAndRecommendationCount(diningId, recommendationCount))
            .willReturn(List.of());
        given(diningParticipantRepository.findByDiningIdAndUserId(diningId, userId)).willReturn(Optional.of(participant));

        // when // then
        assertThatThrownBy(() -> diningService.getRestaurantVoteDetail(userId, groupId, diningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ONLY_ATTEND_PARTICIPANT_CAN_VOTE);
    }

    @Test
    @DisplayName("식당 정보가 없으면 예외가 발생한다.")
    void getRestaurantVoteDetail_restaurantNotFound() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long recommendRestaurantId = 400L;
        String restaurantId = "restaurant-999";
        Integer recommendationCount = 1;

        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.createWithRecommendationCount(diningId, group, DiningStatus.RESTAURANT_VOTING, recommendationCount);
        DiningParticipant participant = DiningParticipantFixture.create(300L, dining, user, AttendanceVoteStatus.ATTEND);

        RecommendRestaurant recommendRestaurant = RecommendRestaurantFixture.create(
            recommendRestaurantId, dining, restaurantId, recommendationCount
        );

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(recommendRestaurantRepository.findByDiningIdAndRecommendationCount(diningId, recommendationCount))
            .willReturn(List.of(recommendRestaurant));
        given(diningParticipantRepository.findByDiningIdAndUserId(diningId, userId)).willReturn(Optional.of(participant));
        given(restaurantRepository.findById(restaurantId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> diningService.getRestaurantVoteDetail(userId, groupId, diningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", RESTAURANT_NOT_FOUND);

        then(restaurantRepository).should().findById(restaurantId);
    }

    @Test
    @DisplayName("여러 추천 장소의 투표 현황을 조회할 수 있다.")
    void getRestaurantVoteDetail_multipleRestaurants() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Integer recommendationCount = 1;

        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.createWithRecommendationCount(diningId, group, DiningStatus.RESTAURANT_VOTING, recommendationCount);
        DiningParticipant participant = DiningParticipantFixture.create(300L, dining, user, AttendanceVoteStatus.ATTEND);

        RecommendRestaurant recommendRestaurant1 = RecommendRestaurantFixture.create(
            400L, dining, "restaurant-001", recommendationCount, 10, 2
        );
        RecommendRestaurant recommendRestaurant2 = RecommendRestaurantFixture.create(
            401L, dining, "restaurant-002", recommendationCount, 8, 3
        );
        RecommendRestaurant recommendRestaurant3 = RecommendRestaurantFixture.create(
            402L, dining, "restaurant-003", recommendationCount, 5, 5
        );

        Restaurant restaurant1 = RestaurantFixture.create("restaurant-001", "한식당");
        Restaurant restaurant2 = RestaurantFixture.create("restaurant-002", "일식당");
        Restaurant restaurant3 = RestaurantFixture.create("restaurant-003", "중식당");

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(recommendRestaurantRepository.findByDiningIdAndRecommendationCount(diningId, recommendationCount))
            .willReturn(List.of(recommendRestaurant1, recommendRestaurant2, recommendRestaurant3));
        given(diningParticipantRepository.findByDiningIdAndUserId(diningId, userId)).willReturn(Optional.of(participant));
        given(restaurantRepository.findById("restaurant-001")).willReturn(Optional.of(restaurant1));
        given(restaurantRepository.findById("restaurant-002")).willReturn(Optional.of(restaurant2));
        given(restaurantRepository.findById("restaurant-003")).willReturn(Optional.of(restaurant3));
        given(recommendRestaurantVoteRepository.findByUserIdAndRecommendRestaurantId(userId, 400L))
            .willReturn(Optional.empty());
        given(recommendRestaurantVoteRepository.findByUserIdAndRecommendRestaurantId(userId, 401L))
            .willReturn(Optional.empty());
        given(recommendRestaurantVoteRepository.findByUserIdAndRecommendRestaurantId(userId, 402L))
            .willReturn(Optional.empty());

        // when
        List<RestaurantVoteDetailResponse> result = diningService.getRestaurantVoteDetail(userId, groupId, diningId);

        // then
        assertThat(result).hasSize(3)
            .extracting("recommendRestaurantsId", "restaurantsName", "likeCount", "dislikeCount")
            .containsExactlyInAnyOrder(
                tuple(400L, "한식당", 10, 2),
                tuple(401L, "일식당", 8, 3),
                tuple(402L, "중식당", 5, 5)
            );
    }

    @Test
    @DisplayName("확정 상태의 회식에서도 장소 투표 현황을 조회할 수 있다.")
    void getRestaurantVoteDetail_confirmedDiningStatus() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long recommendRestaurantId = 400L;
        String restaurantId = "restaurant-001";
        Integer recommendationCount = 1;

        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.createWithRecommendationCount(diningId, group, DiningStatus.CONFIRMED, recommendationCount);
        DiningParticipant participant = DiningParticipantFixture.create(300L, dining, user, AttendanceVoteStatus.ATTEND);

        RecommendRestaurant recommendRestaurant = RecommendRestaurantFixture.create(
            recommendRestaurantId, dining, restaurantId, recommendationCount, 7, 1
        );

        Restaurant restaurant = RestaurantFixture.create(restaurantId, "확정된 식당");

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(recommendRestaurantRepository.findByDiningIdAndRecommendationCount(diningId, recommendationCount))
            .willReturn(List.of(recommendRestaurant));
        given(diningParticipantRepository.findByDiningIdAndUserId(diningId, userId)).willReturn(Optional.of(participant));
        given(restaurantRepository.findById(restaurantId)).willReturn(Optional.of(restaurant));
        given(recommendRestaurantVoteRepository.findByUserIdAndRecommendRestaurantId(userId, recommendRestaurantId))
            .willReturn(Optional.empty());

        // when
        List<RestaurantVoteDetailResponse> result = diningService.getRestaurantVoteDetail(userId, groupId, diningId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0))
            .extracting("recommendRestaurantsId", "restaurantsName", "likeCount", "dislikeCount")
            .contains(recommendRestaurantId, "확정된 식당", 7, 1);
    }

    @Test
    @DisplayName("확정된 회식 장소를 조회한다.")
    void getDiningConfirmed_success() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long recommendRestaurantId = 500L;
        String restaurantId = "6976b54010e1fa815903d4ce";
        Integer recommendationCount = 1;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.createWithRecommendationCount(diningId, group, DiningStatus.CONFIRMED, recommendationCount);
        RecommendRestaurant confirmedRestaurant = RecommendRestaurantFixture.createConfirmed(
            recommendRestaurantId, dining, restaurantId, recommendationCount
        );
        Restaurant restaurant = RestaurantFixture.create(restaurantId, "확정된 맛집");

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(recommendRestaurantRepository.findConfirmedRecommendRestaurant(diningId, recommendationCount))
            .willReturn(Optional.of(confirmedRestaurant));
        given(restaurantRepository.findById(restaurantId)).willReturn(Optional.of(restaurant));

        // when
        DiningConfirmedResponse result = diningService.getDiningConfirmed(userId, groupId, diningId);

        // then
        assertThat(result)
            .extracting(
                "recommendRestaurantsId",
                "restaurantsName",
                "reasoningDescription",
                "phoneNumber",
                "latitude",
                "longitude"
            )
            .contains(
                recommendRestaurantId,
                "확정된 맛집",
                "AI가 추천한 최고의 식당입니다.",
                "02-1234-5678",
                "37.5012",
                "127.0396"
            );

        then(userGroupRepository).should().existsByUserIdAndGroupId(userId, groupId);
        then(diningRepository).should().findById(diningId);
        then(recommendRestaurantRepository).should().findConfirmedRecommendRestaurant(diningId, recommendationCount);
        then(restaurantRepository).should().findById(restaurantId);
    }

    @Test
    @DisplayName("그룹 멤버가 아닌 사용자는 확정된 회식 장소를 조회할 수 없다.")
    void getDiningConfirmed_userNotGroupMember() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(false);

        // when // then
        assertThatThrownBy(() -> diningService.getDiningConfirmed(userId, groupId, diningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", USER_NOT_GROUP_MEMBER);

        then(userGroupRepository).should().existsByUserIdAndGroupId(userId, groupId);
        then(diningRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 회식의 확정 장소를 조회할 수 없다.")
    void getDiningConfirmed_diningNotFound() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 999L;

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> diningService.getDiningConfirmed(userId, groupId, diningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DINING_NOT_FOUND);

        then(userGroupRepository).should().existsByUserIdAndGroupId(userId, groupId);
        then(diningRepository).should().findById(diningId);
        then(recommendRestaurantRepository).should(never()).findConfirmedRecommendRestaurant(any(), any());
    }

    @Test
    @DisplayName("장소 확정이 되지 않은 회식의 확정 장소를 조회할 수 없다.")
    void getDiningConfirmed_diningNotConfirmed() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.RESTAURANT_VOTING);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));

        // when // then
        assertThatThrownBy(() -> diningService.getDiningConfirmed(userId, groupId, diningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DINING_NOT_CONFIRMED);

        then(userGroupRepository).should().existsByUserIdAndGroupId(userId, groupId);
        then(diningRepository).should().findById(diningId);
        then(recommendRestaurantRepository).should(never()).findConfirmedRecommendRestaurant(any(), any());
    }

    @Test
    @DisplayName("참석 투표 중인 회식의 확정 장소를 조회할 수 없다.")
    void getDiningConfirmed_attendanceVotingStatusCannotView() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.ATTENDANCE_VOTING);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));

        // when // then
        assertThatThrownBy(() -> diningService.getDiningConfirmed(userId, groupId, diningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DINING_NOT_CONFIRMED);

        then(userGroupRepository).should().existsByUserIdAndGroupId(userId, groupId);
        then(diningRepository).should().findById(diningId);
    }

    @Test
    @DisplayName("확정된 추천 식당이 없으면 조회할 수 없다.")
    void getDiningConfirmed_recommendRestaurantNotFound() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Integer recommendationCount = 1;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.createWithRecommendationCount(diningId, group, DiningStatus.CONFIRMED, recommendationCount);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(recommendRestaurantRepository.findConfirmedRecommendRestaurant(diningId, recommendationCount))
            .willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> diningService.getDiningConfirmed(userId, groupId, diningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", RECOMMEND_RESTAURANT_NOT_FOUND);

        then(userGroupRepository).should().existsByUserIdAndGroupId(userId, groupId);
        then(diningRepository).should().findById(diningId);
        then(recommendRestaurantRepository).should().findConfirmedRecommendRestaurant(diningId, recommendationCount);
        then(restaurantRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("식당 정보가 없으면 확정 장소를 조회할 수 없다.")
    void getDiningConfirmed_restaurantNotFound() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long recommendRestaurantId = 500L;
        String restaurantId = "6976b54010e1fa815903d4ce";
        Integer recommendationCount = 1;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.createWithRecommendationCount(diningId, group, DiningStatus.CONFIRMED, recommendationCount);
        RecommendRestaurant confirmedRestaurant = RecommendRestaurantFixture.createConfirmed(
            recommendRestaurantId, dining, restaurantId, recommendationCount
        );

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(recommendRestaurantRepository.findConfirmedRecommendRestaurant(diningId, recommendationCount))
            .willReturn(Optional.of(confirmedRestaurant));
        given(restaurantRepository.findById(restaurantId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> diningService.getDiningConfirmed(userId, groupId, diningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", RESTAURANT_NOT_FOUND);

        then(userGroupRepository).should().existsByUserIdAndGroupId(userId, groupId);
        then(diningRepository).should().findById(diningId);
        then(recommendRestaurantRepository).should().findConfirmedRecommendRestaurant(diningId, recommendationCount);
        then(restaurantRepository).should().findById(restaurantId);
    }

    @Test
    @DisplayName("그룹장이 회식 장소를 확정한다.")
    void confirmDiningRestaurant_success() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long recommendRestaurantId = 500L;
        String restaurantId = "6976b54010e1fa815903d4ce";
        Integer recommendationCount = 1;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.createWithRecommendationCount(
            diningId, group, DiningStatus.RESTAURANT_VOTING, recommendationCount
        );
        RecommendRestaurant recommendRestaurant = RecommendRestaurantFixture.create(
            recommendRestaurantId, dining, restaurantId, recommendationCount
        );
        Restaurant restaurant = RestaurantFixture.create(restaurantId, "확정할 맛집");

        given(userGroupRepository.existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER))
            .willReturn(true);
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group));
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(recommendRestaurantRepository.findById(recommendRestaurantId))
            .willReturn(Optional.of(recommendRestaurant));
        given(recommendRestaurantRepository.existsByDiningIdAndRecommendationCountAndConfirmedStatusTrue(
            diningId, recommendationCount)).willReturn(false);
        given(restaurantRepository.findById(restaurantId)).willReturn(Optional.of(restaurant));
        willDoNothing().given(aiService).sendConfirmRestaurant(any(Group.class), any(Dining.class), any(String.class), any(RecommendRestaurant.class));

        // when
        DiningConfirmedResponse result = diningService.confirmDiningRestaurant(
            userId, groupId, diningId, recommendRestaurantId
        );

        // then
        assertThat(result)
            .extracting(
                "recommendRestaurantsId",
                "restaurantsName",
                "reasoningDescription",
                "phoneNumber",
                "latitude",
                "longitude"
            )
            .contains(
                recommendRestaurantId,
                "확정할 맛집",
                "AI가 추천한 식당입니다.",
                "02-1234-5678",
                "37.5012",
                "127.0396"
            );

        assertThat(recommendRestaurant.isConfirmed()).isTrue();
        assertThat(dining.getDiningStatus()).isEqualTo(DiningStatus.CONFIRMED);

        then(userGroupRepository).should().existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER);
        then(diningRepository).should().findById(diningId);
        then(recommendRestaurantRepository).should().findById(recommendRestaurantId);
        then(recommendRestaurantRepository).should()
            .existsByDiningIdAndRecommendationCountAndConfirmedStatusTrue(diningId, recommendationCount);
        then(restaurantRepository).should().findById(restaurantId);
    }

    @Test
    @DisplayName("그룹장이 아닌 사용자는 회식 장소를 확정할 수 없다.")
    void confirmDiningRestaurant_onlyGroupLeaderCanConfirm() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long recommendRestaurantId = 500L;

        given(userGroupRepository.existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER))
            .willReturn(false);

        // when // then
        assertThatThrownBy(() -> diningService.confirmDiningRestaurant(
            userId, groupId, diningId, recommendRestaurantId
        ))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ONLY_GROUP_LEADER_CAN_CONFIRM);

        then(userGroupRepository).should().existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER);
        then(diningRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 회식의 장소를 확정할 수 없다.")
    void confirmDiningRestaurant_diningNotFound() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 999L;
        Long recommendRestaurantId = 500L;

        given(userGroupRepository.existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER))
            .willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> diningService.confirmDiningRestaurant(
            userId, groupId, diningId, recommendRestaurantId
        ))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DINING_NOT_FOUND);

        then(userGroupRepository).should().existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER);
        then(diningRepository).should().findById(diningId);
        then(recommendRestaurantRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 추천 식당을 확정할 수 없다.")
    void confirmDiningRestaurant_recommendRestaurantNotFound() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long recommendRestaurantId = 999L;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.RESTAURANT_VOTING);

        given(userGroupRepository.existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER))
            .willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(recommendRestaurantRepository.findById(recommendRestaurantId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> diningService.confirmDiningRestaurant(
            userId, groupId, diningId, recommendRestaurantId
        ))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", RECOMMEND_RESTAURANT_NOT_FOUND);

        then(userGroupRepository).should().existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER);
        then(diningRepository).should().findById(diningId);
        then(recommendRestaurantRepository).should().findById(recommendRestaurantId);
    }

    @Test
    @DisplayName("이미 확정된 추천 식당은 다시 확정할 수 없다.")
    void confirmDiningRestaurant_alreadyConfirmed() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long recommendRestaurantId = 500L;
        String restaurantId = "6976b54010e1fa815903d4ce";
        Integer recommendationCount = 1;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.createWithRecommendationCount(
            diningId, group, DiningStatus.CONFIRMED, recommendationCount
        );
        RecommendRestaurant confirmedRestaurant = RecommendRestaurantFixture.createConfirmed(
            recommendRestaurantId, dining, restaurantId, recommendationCount
        );

        given(userGroupRepository.existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER))
            .willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(recommendRestaurantRepository.findById(recommendRestaurantId))
            .willReturn(Optional.of(confirmedRestaurant));

        // when // then
        assertThatThrownBy(() -> diningService.confirmDiningRestaurant(
            userId, groupId, diningId, recommendRestaurantId
        ))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", RECOMMEND_RESTAURANT_ALREADY_CONFIRMED);

        then(userGroupRepository).should().existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER);
        then(diningRepository).should().findById(diningId);
        then(recommendRestaurantRepository).should().findById(recommendRestaurantId);
        then(recommendRestaurantRepository).should(never())
            .existsByDiningIdAndRecommendationCountAndConfirmedStatusTrue(any(), any());
    }

    @Test
    @DisplayName("이미 다른 추천 식당이 확정된 경우 확정할 수 없다.")
    void confirmDiningRestaurant_anotherRestaurantAlreadyConfirmed() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long recommendRestaurantId = 500L;
        String restaurantId = "6976b54010e1fa815903d4ce";
        Integer recommendationCount = 1;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.createWithRecommendationCount(
            diningId, group, DiningStatus.RESTAURANT_VOTING, recommendationCount
        );
        RecommendRestaurant recommendRestaurant = RecommendRestaurantFixture.create(
            recommendRestaurantId, dining, restaurantId, recommendationCount
        );

        given(userGroupRepository.existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER))
            .willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(recommendRestaurantRepository.findById(recommendRestaurantId))
            .willReturn(Optional.of(recommendRestaurant));
        given(recommendRestaurantRepository.existsByDiningIdAndRecommendationCountAndConfirmedStatusTrue(
            diningId, recommendationCount)).willReturn(true);

        // when // then
        assertThatThrownBy(() -> diningService.confirmDiningRestaurant(
            userId, groupId, diningId, recommendRestaurantId
        ))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ANOTHER_RESTAURANT_ALREADY_CONFIRMED);

        then(userGroupRepository).should().existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER);
        then(diningRepository).should().findById(diningId);
        then(recommendRestaurantRepository).should().findById(recommendRestaurantId);
        then(recommendRestaurantRepository).should()
            .existsByDiningIdAndRecommendationCountAndConfirmedStatusTrue(diningId, recommendationCount);
        then(restaurantRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("식당 정보가 없으면 회식 장소를 확정할 수 없다.")
    void confirmDiningRestaurant_restaurantNotFound() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;
        Long recommendRestaurantId = 500L;
        String restaurantId = "6976b54010e1fa815903d4ce";
        Integer recommendationCount = 1;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.createWithRecommendationCount(
            diningId, group, DiningStatus.RESTAURANT_VOTING, recommendationCount
        );
        RecommendRestaurant recommendRestaurant = RecommendRestaurantFixture.create(
            recommendRestaurantId, dining, restaurantId, recommendationCount
        );

        given(userGroupRepository.existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER))
            .willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(recommendRestaurantRepository.findById(recommendRestaurantId))
            .willReturn(Optional.of(recommendRestaurant));
        given(recommendRestaurantRepository.existsByDiningIdAndRecommendationCountAndConfirmedStatusTrue(
            diningId, recommendationCount)).willReturn(false);
        given(restaurantRepository.findById(restaurantId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> diningService.confirmDiningRestaurant(
            userId, groupId, diningId, recommendRestaurantId
        ))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", RESTAURANT_NOT_FOUND);

        then(userGroupRepository).should().existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER);
        then(diningRepository).should().findById(diningId);
        then(recommendRestaurantRepository).should().findById(recommendRestaurantId);
        then(recommendRestaurantRepository).should()
            .existsByDiningIdAndRecommendationCountAndConfirmedStatusTrue(diningId, recommendationCount);
        then(restaurantRepository).should().findById(restaurantId);
    }

    @Test
    @DisplayName("그룹장이 추천 장소를 새로고침한다.")
    void refreshRecommendRestaurants_success() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;

        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.createWithRecommendationCount(diningId, group, DiningStatus.RESTAURANT_VOTING, 1);
        DiningParticipant participant = DiningParticipantFixture.create(300L, dining, user, AttendanceVoteStatus.ATTEND);

        given(userGroupRepository.existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER))
            .willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group));
        given(diningParticipantRepository.findAllByDiningAndAttendanceVoteStatus(dining, AttendanceVoteStatus.ATTEND))
            .willReturn(List.of(participant));

        // when
        diningService.refreshRecommendRestaurants(userId, groupId, diningId);

        // then
        assertThat(dining.getDiningStatus()).isEqualTo(DiningStatus.RECOMMENDATION_PENDING);

        then(userGroupRepository).should().existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER);
        then(diningRepository).should().findById(diningId);
        then(groupRepository).should().findById(groupId);
        then(commonEventPublisher).should().publish(eq(EventType.RESTAURANT_RECOMMENDATION_REFRESH), any(RecommendationRefreshEventPayload.class));
    }

    @Test
    @DisplayName("그룹장이 아닌 사용자는 추천 장소를 새로고침할 수 없다.")
    void refreshRecommendRestaurants_onlyGroupLeaderCanRefresh() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;

        given(userGroupRepository.existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER))
            .willReturn(false);

        // when // then
        assertThatThrownBy(() -> diningService.refreshRecommendRestaurants(userId, groupId, diningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ONLY_GROUP_LEADER_CAN_REFRESH);

        then(userGroupRepository).should().existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER);
        then(diningRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 회식의 추천 장소는 새로고침할 수 없다.")
    void refreshRecommendRestaurants_diningNotFound() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 999L;

        given(userGroupRepository.existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER))
            .willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> diningService.refreshRecommendRestaurants(userId, groupId, diningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DINING_NOT_FOUND);

        then(userGroupRepository).should().existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER);
        then(diningRepository).should().findById(diningId);
        then(groupRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("식당 투표 중이 아닌 회식의 추천 장소는 새로고침할 수 없다.")
    void refreshRecommendRestaurants_notRestaurantVotingStatus() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.CONFIRMED);

        given(userGroupRepository.existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER))
            .willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));

        // when // then
        assertThatThrownBy(() -> diningService.refreshRecommendRestaurants(userId, groupId, diningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", RECOMMEND_REFRESH_ONLY_IN_RESTAURANT_VOTING);

        then(userGroupRepository).should().existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER);
        then(diningRepository).should().findById(diningId);
        then(groupRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("참석 투표 중인 회식의 추천 장소는 새로고침할 수 없다.")
    void refreshRecommendRestaurants_attendanceVotingStatusCannotRefresh() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.ATTENDANCE_VOTING);

        given(userGroupRepository.existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER))
            .willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));

        // when // then
        assertThatThrownBy(() -> diningService.refreshRecommendRestaurants(userId, groupId, diningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", RECOMMEND_REFRESH_ONLY_IN_RESTAURANT_VOTING);

        then(userGroupRepository).should().existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER);
        then(diningRepository).should().findById(diningId);
    }

    @Test
    @DisplayName("완료된 회식의 추천 장소는 새로고침할 수 없다.")
    void refreshRecommendRestaurants_completeStatusCannotRefresh() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;

        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.COMPLETE);

        given(userGroupRepository.existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER))
            .willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));

        // when // then
        assertThatThrownBy(() -> diningService.refreshRecommendRestaurants(userId, groupId, diningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", RECOMMEND_REFRESH_ONLY_IN_RESTAURANT_VOTING);

        then(userGroupRepository).should().existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER);
        then(diningRepository).should().findById(diningId);
    }

    @Test
    @DisplayName("존재하지 않는 그룹의 추천 장소는 새로고침할 수 없다.")
    void refreshRecommendRestaurants_groupNotFound() {
        // given
        Long userId = 1L;
        Long groupId = 999L;
        Long diningId = 200L;

        Group group = GroupFixture.create(100L, "맛집탐방대");
        Dining dining = DiningFixture.create(diningId, group, DiningStatus.RESTAURANT_VOTING);

        given(userGroupRepository.existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER))
            .willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(groupRepository.findById(groupId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> diningService.refreshRecommendRestaurants(userId, groupId, diningId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", GROUP_NOT_FOUND);

        then(userGroupRepository).should().existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER);
        then(diningRepository).should().findById(diningId);
        then(groupRepository).should().findById(groupId);
        then(commonEventPublisher).should(never()).publish(any(), any());
    }

    @Test
    @DisplayName("여러 참석자가 있는 회식의 추천 장소를 새로고침한다.")
    void refreshRecommendRestaurants_withMultipleAttendees() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long diningId = 200L;

        User user1 = UserFixture.create(userId);
        User user2 = UserFixture.create(2L);
        User user3 = UserFixture.create(3L);
        Group group = GroupFixture.create(groupId, "맛집탐방대");
        Dining dining = DiningFixture.createWithRecommendationCount(diningId, group, DiningStatus.RESTAURANT_VOTING, 1);
        DiningParticipant participant1 = DiningParticipantFixture.create(300L, dining, user1, AttendanceVoteStatus.ATTEND);
        DiningParticipant participant2 = DiningParticipantFixture.create(301L, dining, user2, AttendanceVoteStatus.ATTEND);
        DiningParticipant participant3 = DiningParticipantFixture.create(302L, dining, user3, AttendanceVoteStatus.ATTEND);

        given(userGroupRepository.existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER))
            .willReturn(true);
        given(diningRepository.findById(diningId)).willReturn(Optional.of(dining));
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group));
        given(diningParticipantRepository.findAllByDiningAndAttendanceVoteStatus(dining, AttendanceVoteStatus.ATTEND))
            .willReturn(List.of(participant1, participant2, participant3));

        // when
        diningService.refreshRecommendRestaurants(userId, groupId, diningId);

        // then
        assertThat(dining.getDiningStatus()).isEqualTo(DiningStatus.RECOMMENDATION_PENDING);
        then(commonEventPublisher).should().publish(eq(EventType.RESTAURANT_RECOMMENDATION_REFRESH), any(RecommendationRefreshEventPayload.class));
    }
}
