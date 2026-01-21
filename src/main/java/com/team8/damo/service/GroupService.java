package com.team8.damo.service;

import com.team8.damo.entity.Group;
import com.team8.damo.entity.User;
import com.team8.damo.entity.UserGroup;
import com.team8.damo.exception.CustomException;
import com.team8.damo.repository.GroupRepository;
import com.team8.damo.repository.UserGroupRepository;
import com.team8.damo.repository.UserRepository;
import com.team8.damo.service.request.GroupCreateServiceRequest;
import com.team8.damo.service.response.UserGroupResponse;
import com.team8.damo.util.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.team8.damo.exception.errorcode.ErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService {
    private final Snowflake snowflake;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;

    @Transactional
    public Long createGroup(Long userId, GroupCreateServiceRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        Group group = request.toEntity(snowflake.nextId());
        UserGroup groupLeader = UserGroup.createLeader(snowflake.nextId(), user, group);

        groupRepository.save(group);
        userGroupRepository.save(groupLeader);
        return group.getId();
    }

    public List<UserGroupResponse> getGroupList(Long userId) {
        List<UserGroup> userGroups = userGroupRepository.findAllByUserIdWithGroup(userId);
        return userGroups.stream()
            .map(UserGroupResponse::of)
            .toList();
    }
}
