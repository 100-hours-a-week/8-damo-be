package com.team8.damo.service;

import com.team8.damo.entity.*;
import com.team8.damo.entity.enumeration.AttendanceVoteStatus;
import com.team8.damo.entity.enumeration.DiningStatus;
import com.team8.damo.entity.enumeration.GroupRole;
import com.team8.damo.exception.CustomException;
import com.team8.damo.fixture.DiningFixture;
import com.team8.damo.fixture.GroupFixture;
import com.team8.damo.fixture.UserFixture;
import com.team8.damo.repository.*;
import com.team8.damo.service.response.GroupDetailResponse;
import com.team8.damo.service.response.UserGroupResponse;
import com.team8.damo.service.request.GroupCreateServiceRequest;
import com.team8.damo.util.QrCodeGenerator;
import com.team8.damo.util.Snowflake;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static com.team8.damo.exception.errorcode.ErrorCode.USER_NOT_FOUND;
import static com.team8.damo.exception.errorcode.ErrorCode.USER_NOT_GROUP_MEMBER;
import static com.team8.damo.exception.errorcode.ErrorCode.GROUP_NOT_FOUND;
import static com.team8.damo.exception.errorcode.ErrorCode.DUPLICATE_GROUP_MEMBER;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private Snowflake snowflake;

    @Mock
    private QrCodeGenerator qrCodeGenerator;

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
    private GroupService groupService;

    @Test
    @DisplayName("그룹을 성공적으로 생성한다.")
    void createGroup_success() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long userGroupId = 200L;
        String imagePath = "groups/profile/group-100.png";

        GroupCreateServiceRequest request = new GroupCreateServiceRequest(
            "맛집탐방대",
            "서울 맛집을 함께 다니는 모임",
            37.5665,
            126.9780,
            imagePath
        );

        User user = UserFixture.create(userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(snowflake.nextId()).willReturn(groupId, userGroupId);

        // when
        Long result = groupService.createGroup(userId, request);

        // then
        assertThat(result).isEqualTo(groupId);

        ArgumentCaptor<Group> groupCaptor = ArgumentCaptor.forClass(Group.class);
        ArgumentCaptor<UserGroup> userGroupCaptor = ArgumentCaptor.forClass(UserGroup.class);

        then(groupRepository).should().save(groupCaptor.capture());
        then(userGroupRepository).should().save(userGroupCaptor.capture());

        Group savedGroup = groupCaptor.getValue();
        assertThat(savedGroup.getId()).isEqualTo(groupId);
        assertThat(savedGroup.getName()).isEqualTo("맛집탐방대");
        assertThat(savedGroup.getIntroduction()).isEqualTo("서울 맛집을 함께 다니는 모임");
        assertThat(savedGroup.getLatitude()).isEqualTo(37.5665);
        assertThat(savedGroup.getLongitude()).isEqualTo(126.9780);
        assertThat(savedGroup.getImagePath()).isEqualTo(imagePath);

        UserGroup savedUserGroup = userGroupCaptor.getValue();
        assertThat(savedUserGroup.getId()).isEqualTo(userGroupId);
        assertThat(savedUserGroup.getUser()).isEqualTo(user);
        assertThat(savedUserGroup.getGroup()).isEqualTo(savedGroup);
        assertThat(savedUserGroup.getRole()).isEqualTo(GroupRole.LEADER);
    }

    @Test
    @DisplayName("소개글 없이 그룹을 생성할 수 있다.")
    void createGroup_withoutIntroduction() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long userGroupId = 200L;

        GroupCreateServiceRequest request = new GroupCreateServiceRequest(
            "맛집탐방대",
            null,
            37.5665,
            126.9780,
            null
        );

        User user = UserFixture.create(userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(snowflake.nextId()).willReturn(groupId, userGroupId);

        // when
        Long result = groupService.createGroup(userId, request);

        // then
        assertThat(result).isEqualTo(groupId);

        ArgumentCaptor<Group> groupCaptor = ArgumentCaptor.forClass(Group.class);
        then(groupRepository).should().save(groupCaptor.capture());

        Group savedGroup = groupCaptor.getValue();
        assertThat(savedGroup.getIntroduction()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 그룹을 생성할 수 없다.")
    void createGroup_userNotFound() {
        // given
        Long userId = 999L;

        GroupCreateServiceRequest request = new GroupCreateServiceRequest(
            "맛집탐방대",
            "서울 맛집을 함께 다니는 모임",
            37.5665,
            126.9780,
            "groups/profile/group-999.png"
        );

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> groupService.createGroup(userId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", USER_NOT_FOUND);

        then(userRepository).should().findById(userId);
        then(groupRepository).should(never()).save(any());
        then(userGroupRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("사용자가 속한 그룹 목록을 성공적으로 조회한다.")
    void getGroupList_success() {
        // given
        Long userId = 1L;
        User user = UserFixture.create(userId);

        Group group1 = GroupFixture.create(100L, "맛집탐방대", "서울 맛집 모임");
        Group group2 = GroupFixture.create(101L, "카페투어", "카페 탐방 모임");

        UserGroup userGroup1 = UserGroup.createLeader(200L, user, group1);
        UserGroup userGroup2 = UserGroup.builder()
            .id(201L)
            .user(user)
            .group(group2)
            .role(GroupRole.PARTICIPANT)
            .build();

        given(userGroupRepository.findAllByUserIdWithGroup(userId))
            .willReturn(List.of(userGroup1, userGroup2));

        // when
        List<UserGroupResponse> result = groupService.getGroupList(userId);

        // then
        assertThat(result).hasSize(2);
        assertThat(result)
            .extracting("groupId", "name", "introduction")
            .containsExactly(
                tuple(100L, "맛집탐방대", "서울 맛집 모임"),
                tuple(101L, "카페투어", "카페 탐방 모임")
            );

        then(userGroupRepository).should().findAllByUserIdWithGroup(userId);
    }

    @Test
    @DisplayName("속한 그룹이 없으면 빈 목록을 반환한다.")
    void getGroupList_emptyList() {
        // given
        Long userId = 1L;

        given(userGroupRepository.findAllByUserIdWithGroup(userId))
            .willReturn(List.of());

        // when
        List<UserGroupResponse> result = groupService.getGroupList(userId);

        // then
        assertThat(result).isEmpty();

        then(userGroupRepository).should().findAllByUserIdWithGroup(userId);
    }

    @Test
    @DisplayName("그룹장이 그룹 상세 정보를 조회한다.")
    void getGroupDetail_asLeader() {
        // given
        Long userId = 1L;
        Long groupId = 100L;

        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대", "서울 맛집 모임");
        UserGroup userGroup = UserGroup.createLeader(200L, user, group);

        given(userGroupRepository.findByUserIdAndGroupId(userId, groupId))
            .willReturn(Optional.of(userGroup));
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group));

        // when
        GroupDetailResponse result = groupService.getGroupDetail(userId, groupId);

        // then
        assertThat(result)
            .extracting("name", "introduction", "participantsCount", "isGroupLeader")
            .contains("맛집탐방대", "서울 맛집 모임", 1, true);

        then(userGroupRepository).should().findByUserIdAndGroupId(userId, groupId);
        then(groupRepository).should().findById(groupId);
    }

    @Test
    @DisplayName("일반 참여자가 그룹 상세 정보를 조회한다.")
    void getGroupDetail_asParticipant() {
        // given
        Long userId = 1L;
        Long groupId = 100L;

        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대", "서울 맛집 모임");
        UserGroup userGroup = UserGroup.builder()
            .id(200L)
            .user(user)
            .group(group)
            .role(GroupRole.PARTICIPANT)
            .build();

        given(userGroupRepository.findByUserIdAndGroupId(userId, groupId))
            .willReturn(Optional.of(userGroup));
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group));

        // when
        GroupDetailResponse result = groupService.getGroupDetail(userId, groupId);

        // then
        assertThat(result)
            .extracting("name", "introduction", "participantsCount", "isGroupLeader")
            .contains("맛집탐방대", "서울 맛집 모임", 1, false);

        then(userGroupRepository).should().findByUserIdAndGroupId(userId, groupId);
        then(groupRepository).should().findById(groupId);
    }

    @Test
    @DisplayName("그룹 멤버가 아닌 사용자도 그룹 상세 정보를 조회할 수 있다.")
    void getGroupDetail_asNonMember() {
        // given
        Long userId = 1L;
        Long groupId = 100L;

        Group group = GroupFixture.create(groupId, "맛집탐방대", "서울 맛집 모임");

        given(userGroupRepository.findByUserIdAndGroupId(userId, groupId))
            .willReturn(Optional.empty());
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group));

        // when
        GroupDetailResponse result = groupService.getGroupDetail(userId, groupId);

        // then
        assertThat(result)
            .extracting("name", "introduction", "participantsCount", "isGroupLeader")
            .contains("맛집탐방대", "서울 맛집 모임", 1, false);

        then(userGroupRepository).should().findByUserIdAndGroupId(userId, groupId);
        then(groupRepository).should().findById(groupId);
    }

    @Test
    @DisplayName("존재하지 않는 그룹의 상세 정보를 조회할 수 없다.")
    void getGroupDetail_groupNotFound() {
        // given
        Long userId = 1L;
        Long groupId = 999L;

        given(userGroupRepository.findByUserIdAndGroupId(userId, groupId))
            .willReturn(Optional.empty());
        given(groupRepository.findById(groupId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> groupService.getGroupDetail(userId, groupId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", GROUP_NOT_FOUND);

        then(userGroupRepository).should().findByUserIdAndGroupId(userId, groupId);
        then(groupRepository).should().findById(groupId);
    }

    @Test
    @DisplayName("그룹에 성공적으로 참여한다. (진행중인 참석 투표 없음)")
    void attendGroup_success_noAttendanceVoting() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long userGroupId = 200L;

        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대", "서울 맛집 모임");

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group));
        given(snowflake.nextId()).willReturn(userGroupId);
        given(diningRepository.findAllByGroupIdAndDiningStatus(groupId, DiningStatus.ATTENDANCE_VOTING))
            .willReturn(List.of());

        // when
        Long result = groupService.attendGroup(userId, groupId);

        // then
        assertThat(result).isEqualTo(groupId);

        ArgumentCaptor<UserGroup> userGroupCaptor = ArgumentCaptor.forClass(UserGroup.class);
        then(userGroupRepository).should().save(userGroupCaptor.capture());

        UserGroup savedUserGroup = userGroupCaptor.getValue();
        assertThat(savedUserGroup)
            .extracting("id", "user", "group", "role")
            .contains(userGroupId, user, group, GroupRole.PARTICIPANT);

        then(diningRepository).should().findAllByGroupIdAndDiningStatus(groupId, DiningStatus.ATTENDANCE_VOTING);
        then(diningParticipantRepository).should().saveAll(List.of());
        then(groupRepository).should().increaseTotalMembers(groupId);
    }

    @Test
    @DisplayName("그룹 참여 시 진행중인 참석 투표가 있으면 DiningParticipant가 생성된다.")
    void attendGroup_success_withAttendanceVoting() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long userGroupId = 200L;
        Long diningParticipantId1 = 300L;
        Long diningParticipantId2 = 301L;

        User user = UserFixture.create(userId);
        Group group = GroupFixture.create(groupId, "맛집탐방대", "서울 맛집 모임");
        Dining dining1 = DiningFixture.create(1000L, group, DiningStatus.ATTENDANCE_VOTING);
        Dining dining2 = DiningFixture.create(1001L, group, DiningStatus.ATTENDANCE_VOTING);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(groupRepository.findById(groupId)).willReturn(Optional.of(group));
        given(snowflake.nextId()).willReturn(userGroupId, diningParticipantId1, diningParticipantId2);
        given(diningRepository.findAllByGroupIdAndDiningStatus(groupId, DiningStatus.ATTENDANCE_VOTING))
            .willReturn(List.of(dining1, dining2));

        // when
        Long result = groupService.attendGroup(userId, groupId);

        // then
        assertThat(result).isEqualTo(groupId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DiningParticipant>> participantsCaptor = ArgumentCaptor.forClass(List.class);
        then(diningParticipantRepository).should().saveAll(participantsCaptor.capture());

        List<DiningParticipant> savedParticipants = participantsCaptor.getValue();
        assertThat(savedParticipants).hasSize(2);
        assertThat(savedParticipants)
            .extracting("dining", "user", "attendanceVoteStatus")
            .containsExactly(
                tuple(dining1, user, AttendanceVoteStatus.PENDING),
                tuple(dining2, user, AttendanceVoteStatus.PENDING)
            );

        then(groupRepository).should().increaseTotalMembers(groupId);
    }

    @Test
    @DisplayName("이미 참여중인 그룹에는 중복 참여할 수 없다.")
    void attendGroup_duplicateGroupMember() {
        // given
        Long userId = 1L;
        Long groupId = 100L;

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(true);

        // when // then
        assertThatThrownBy(() -> groupService.attendGroup(userId, groupId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DUPLICATE_GROUP_MEMBER);

        then(userGroupRepository).should().existsByUserIdAndGroupId(userId, groupId);
        then(userRepository).should(never()).findById(any());
        then(groupRepository).should(never()).findById(any());
        then(userGroupRepository).should(never()).save(any());
        then(diningRepository).should(never()).findAllByGroupIdAndDiningStatus(any(), any());
        then(diningParticipantRepository).should(never()).saveAll(any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 그룹에 참여할 수 없다.")
    void attendGroup_userNotFound() {
        // given
        Long userId = 999L;
        Long groupId = 100L;

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> groupService.attendGroup(userId, groupId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", USER_NOT_FOUND);

        then(userGroupRepository).should().existsByUserIdAndGroupId(userId, groupId);
        then(userRepository).should().findById(userId);
        then(groupRepository).should(never()).findById(any());
        then(userGroupRepository).should(never()).save(any());
        then(diningRepository).should(never()).findAllByGroupIdAndDiningStatus(any(), any());
        then(diningParticipantRepository).should(never()).saveAll(any());
    }

    @Test
    @DisplayName("존재하지 않는 그룹에는 참여할 수 없다.")
    void attendGroup_groupNotFound() {
        // given
        Long userId = 1L;
        Long groupId = 999L;

        User user = UserFixture.create(userId);

        given(userGroupRepository.existsByUserIdAndGroupId(userId, groupId)).willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(groupRepository.findById(groupId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> groupService.attendGroup(userId, groupId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", GROUP_NOT_FOUND);

        then(userGroupRepository).should().existsByUserIdAndGroupId(userId, groupId);
        then(userRepository).should().findById(userId);
        then(groupRepository).should().findById(groupId);
        then(userGroupRepository).should(never()).save(any());
        then(diningRepository).should(never()).findAllByGroupIdAndDiningStatus(any(), any());
        then(diningParticipantRepository).should(never()).saveAll(any());
    }
}
