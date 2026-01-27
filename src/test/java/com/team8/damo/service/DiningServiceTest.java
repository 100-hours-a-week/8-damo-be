package com.team8.damo.service;

import com.team8.damo.client.AiService;
import com.team8.damo.entity.*;
import com.team8.damo.entity.enumeration.AttendanceVoteStatus;
import com.team8.damo.entity.enumeration.DiningStatus;
import com.team8.damo.entity.enumeration.GroupRole;
import com.team8.damo.entity.enumeration.RestaurantVoteStatus;
import com.team8.damo.exception.CustomException;
import com.team8.damo.fixture.DiningFixture;
import com.team8.damo.fixture.DiningParticipantFixture;
import com.team8.damo.fixture.GroupFixture;
import com.team8.damo.fixture.RecommendRestaurantFixture;
import com.team8.damo.fixture.RecommendRestaurantVoteFixture;
import com.team8.damo.fixture.UserFixture;
import com.team8.damo.repository.*;
import com.team8.damo.service.request.DiningCreateServiceRequest;
import com.team8.damo.service.request.RestaurantVoteServiceRequest;
import com.team8.damo.service.response.DiningResponse;
import com.team8.damo.service.response.RestaurantVoteResponse;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
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
    private ApplicationEventPublisher eventPublisher;

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
        given(diningRepository.countByGroupIdAndDiningStatusNot(groupId, DiningStatus.COMPLETE)).willReturn(0);
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
        given(diningRepository.countByGroupIdAndDiningStatusNot(groupId, DiningStatus.COMPLETE)).willReturn(3);

        // when // then
        assertThatThrownBy(() -> diningService.createDining(userId, groupId, request, now))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DINING_LIMIT_EXCEEDED);

        then(diningRepository).should().countByGroupIdAndDiningStatusNot(groupId, DiningStatus.COMPLETE);
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
        given(diningRepository.countByGroupIdAndDiningStatusNot(groupId, DiningStatus.COMPLETE)).willReturn(2);
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
        given(diningRepository.countByGroupIdAndDiningStatusNot(groupId, DiningStatus.COMPLETE)).willReturn(0);
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
        given(diningRepository.countByGroupIdAndDiningStatusNot(groupId, DiningStatus.COMPLETE)).willReturn(0);
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
        given(diningParticipantRepository.findByDiningIdInAndVotingStatus(List.of(200L, 201L), AttendanceVoteStatus.ATTEND))
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
        given(diningParticipantRepository.findByDiningIdInAndVotingStatus(List.of(), AttendanceVoteStatus.ATTEND))
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
        given(diningParticipantRepository.findByDiningIdInAndVotingStatus(List.of(200L), AttendanceVoteStatus.ATTEND))
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
        given(diningParticipantRepository.findByDiningIdInAndVotingStatus(List.of(200L), AttendanceVoteStatus.ATTEND))
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
        given(diningParticipantRepository.findByDiningIdInAndVotingStatus(List.of(200L), AttendanceVoteStatus.ATTEND))
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
        given(diningRepository.increaseAttendanceVoteDoneCount(diningId)).willReturn(1);
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
        given(diningRepository.increaseAttendanceVoteDoneCount(diningId)).willReturn(1);
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
            .hasFieldOrPropertyWithValue("errorCode", NO_VOTE_PERMISSION);

        then(diningRepository).should().findById(diningId);
        then(diningParticipantRepository).should().findByDiningIdAndUserId(diningId, userId);
        then(diningRepository).should(never()).increaseAttendanceVoteDoneCount(any());
    }

    @Test
    @DisplayName("이미 투표를 완료한 사용자는 다시 투표할 수 없다.")
    void voteAttendance_alreadyVoted() {
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

        // when // then
        assertThatThrownBy(() -> diningService.voteAttendance(userId, groupId, diningId, AttendanceVoteStatus.NON_ATTEND))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ATTENDANCE_VOTE_ALREADY_COMPLETED);

        then(diningRepository).should().findById(diningId);
        then(diningParticipantRepository).should().findByDiningIdAndUserId(diningId, userId);
        then(diningRepository).should(never()).increaseAttendanceVoteDoneCount(any());
    }

    @Test
    @DisplayName("불참으로 이미 투표한 사용자는 다시 투표할 수 없다.")
    void voteAttendance_alreadyVotedNonAttend() {
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

        // when // then
        assertThatThrownBy(() -> diningService.voteAttendance(userId, groupId, diningId, AttendanceVoteStatus.ATTEND))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ATTENDANCE_VOTE_ALREADY_COMPLETED);
    }

    @Test
    @DisplayName("모든 참여자가 투표를 완료하면 회식 상태가 장소 투표로 변경된다.")
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
        given(diningRepository.increaseAttendanceVoteDoneCount(diningId)).willReturn(3);
        given(diningParticipantRepository.countByDiningId(diningId)).willReturn(3);
        given(diningParticipantRepository.findAllByDiningAndVotingStatus(dining, AttendanceVoteStatus.ATTEND))
            .willReturn(List.of(participant));

        // when
        AttendanceVoteStatus result = diningService.voteAttendance(userId, groupId, diningId, AttendanceVoteStatus.ATTEND);

        // then
        assertThat(result).isEqualTo(AttendanceVoteStatus.ATTEND);
        assertThat(dining.getDiningStatus()).isEqualTo(DiningStatus.RESTAURANT_VOTING);
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
        given(diningRepository.increaseAttendanceVoteDoneCount(diningId)).willReturn(2);
        given(diningParticipantRepository.countByDiningId(diningId)).willReturn(5);

        // when
        AttendanceVoteStatus result = diningService.voteAttendance(userId, groupId, diningId, AttendanceVoteStatus.ATTEND);

        // then
        assertThat(result).isEqualTo(AttendanceVoteStatus.ATTEND);
        assertThat(dining.getDiningStatus()).isEqualTo(DiningStatus.ATTENDANCE_VOTING);
    }

    @Test
    @DisplayName("1명만 있는 그룹에서 투표하면 바로 장소 투표 상태로 변경된다.")
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
        given(diningRepository.increaseAttendanceVoteDoneCount(diningId)).willReturn(1);
        given(diningParticipantRepository.countByDiningId(diningId)).willReturn(1);
        given(diningParticipantRepository.findAllByDiningAndVotingStatus(dining, AttendanceVoteStatus.ATTEND))
            .willReturn(List.of(participant));

        // when
        AttendanceVoteStatus result = diningService.voteAttendance(userId, groupId, diningId, AttendanceVoteStatus.ATTEND);

        // then
        assertThat(result).isEqualTo(AttendanceVoteStatus.ATTEND);
        assertThat(dining.getDiningStatus()).isEqualTo(DiningStatus.RESTAURANT_VOTING);
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
        assertThat(result.restaurantVoteStatus()).isEqualTo(RestaurantVoteStatus.LIKE);

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
        assertThat(result.restaurantVoteStatus()).isEqualTo(RestaurantVoteStatus.DISLIKE);

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
        assertThat(result.restaurantVoteStatus()).isEqualTo(RestaurantVoteStatus.DISLIKE);
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
        assertThat(result.restaurantVoteStatus()).isEqualTo(RestaurantVoteStatus.LIKE);
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
        assertThat(result.restaurantVoteStatus()).isEqualTo(restaurantVoteStatus);

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
        assertThat(result.restaurantVoteStatus()).isEqualTo(restaurantVoteStatus);

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
}
