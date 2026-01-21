package com.team8.damo.service;

import com.team8.damo.entity.Group;
import com.team8.damo.entity.User;
import com.team8.damo.entity.UserGroup;
import com.team8.damo.entity.enumeration.GroupRole;
import com.team8.damo.exception.CustomException;
import com.team8.damo.fixture.UserFixture;
import com.team8.damo.repository.GroupRepository;
import com.team8.damo.repository.UserGroupRepository;
import com.team8.damo.repository.UserRepository;
import com.team8.damo.service.request.GroupCreateServiceRequest;
import com.team8.damo.util.Snowflake;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static com.team8.damo.exception.errorcode.ErrorCode.USER_NOT_FOUND;
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
    private UserRepository userRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private UserGroupRepository userGroupRepository;

    @InjectMocks
    private GroupService groupService;

    @Test
    @DisplayName("그룹을 성공적으로 생성한다.")
    void createGroup_success() {
        // given
        Long userId = 1L;
        Long groupId = 100L;
        Long userGroupId = 200L;

        GroupCreateServiceRequest request = new GroupCreateServiceRequest(
            "맛집탐방대",
            "서울 맛집을 함께 다니는 모임",
            37.5665,
            126.9780
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
            126.9780
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
            126.9780
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
}
