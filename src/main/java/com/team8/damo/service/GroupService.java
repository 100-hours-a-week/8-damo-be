package com.team8.damo.service;

import com.team8.damo.entity.Group;
import com.team8.damo.entity.User;
import com.team8.damo.entity.UserGroup;
import com.team8.damo.exception.CustomException;
import com.team8.damo.repository.GroupRepository;
import com.team8.damo.repository.UserGroupRepository;
import com.team8.damo.repository.UserRepository;
import com.team8.damo.service.request.GroupCreateServiceRequest;
import com.team8.damo.service.response.GroupDetailResponse;
import com.team8.damo.entity.enumeration.GroupRole;
import com.team8.damo.service.response.UserGroupResponse;
import com.team8.damo.util.QrCodeGenerator;
import com.team8.damo.util.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.team8.damo.exception.errorcode.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService {
    private final Snowflake snowflake;
    private final QrCodeGenerator qrCodeGenerator;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;

    @Transactional
    public Long createGroup(Long userId, GroupCreateServiceRequest request) {
        User user = findUserBy(userId);

        Group group = request.toEntity(snowflake.nextId());
        UserGroup groupLeader = UserGroup.createLeader(snowflake.nextId(), user, group);

        groupRepository.save(group);
        userGroupRepository.save(groupLeader);

        qrCodeGenerator.generateQrCode(group.getId());
        return group.getId();
    }

    public List<UserGroupResponse> getGroupList(Long userId) {
        List<UserGroup> userGroups = userGroupRepository.findAllByUserIdWithGroup(userId);
        return userGroups.stream()
            .map(UserGroupResponse::of)
            .toList();
    }

    public GroupDetailResponse getGroupDetail(Long userId, Long groupId) {
        UserGroup userGroup = userGroupRepository.findByUserIdAndGroupId(userId, groupId)
            .orElseThrow(() -> new CustomException(USER_NOT_GROUP_MEMBER));

        boolean isGroupLeader = userGroup.getRole() == GroupRole.LEADER;
        return GroupDetailResponse.of(userGroup.getGroup(), isGroupLeader);
    }

    @Transactional
    public Long attendGroup(Long userId, Long groupId) {
        if (userGroupRepository.existsByUserIdAndGroupId(userId, groupId)) {
            throw new CustomException(DUPLICATE_GROUP_MEMBER);
        }

        User user = findUserBy(userId);
        Group group = findGroupBy(groupId);

        UserGroup participant = UserGroup.createParticipant(snowflake.nextId(), user, group);
        userGroupRepository.save(participant);

        groupRepository.increaseTotalMembers(groupId);
        return groupId;
    }

    private Group findGroupBy(Long groupId) {
        return groupRepository.findById(groupId)
            .orElseThrow(() -> new CustomException(GROUP_NOT_FOUND));
    }

    private User findUserBy(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }
}
