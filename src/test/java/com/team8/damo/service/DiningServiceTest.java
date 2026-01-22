package com.team8.damo.service;

import com.team8.damo.entity.Dining;
import com.team8.damo.entity.DiningParticipant;
import com.team8.damo.entity.Group;
import com.team8.damo.entity.User;
import com.team8.damo.entity.UserGroup;
import com.team8.damo.entity.enumeration.DiningStatus;
import com.team8.damo.entity.enumeration.GroupRole;
import com.team8.damo.entity.enumeration.VotingStatus;
import com.team8.damo.exception.CustomException;
import com.team8.damo.fixture.GroupFixture;
import com.team8.damo.fixture.UserFixture;
import com.team8.damo.repository.DiningParticipantRepository;
import com.team8.damo.repository.DiningRepository;
import com.team8.damo.repository.GroupRepository;
import com.team8.damo.repository.UserGroupRepository;
import com.team8.damo.repository.UserRepository;
import com.team8.damo.service.request.DiningCreateServiceRequest;
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
import java.util.List;
import java.util.Optional;

import static com.team8.damo.exception.errorcode.ErrorCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
            .extracting("id", "user", "votingStatus")
            .containsExactlyInAnyOrder(
                tuple(participantId1, user, VotingStatus.PENDING),
                tuple(participantId2, user2, VotingStatus.PENDING)
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
            .extracting("id", "user", "votingStatus")
            .contains(participantId, user, VotingStatus.PENDING);
    }
}
