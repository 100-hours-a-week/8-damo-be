package com.team8.damo.service;

import com.team8.damo.entity.*;
import com.team8.damo.entity.enumeration.AttendanceVoteStatus;
import com.team8.damo.entity.enumeration.DiningStatus;
import com.team8.damo.entity.enumeration.GroupRole;
import com.team8.damo.exception.CustomException;
import com.team8.damo.repository.*;
import com.team8.damo.service.request.GroupCreateServiceRequest;
import com.team8.damo.service.response.GroupDetailResponse;
import com.team8.damo.service.response.UserGroupResponse;
import com.team8.damo.util.QrCodeGenerator;
import com.team8.damo.util.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
    private final DiningRepository diningRepository;
    private final DiningParticipantRepository diningParticipantRepository;

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

    @Transactional
    public void changeImagePath(Long userId, Long groupId, String imagePath) {
        Group group = findGroupBy(groupId);
        group.changeImagePath(imagePath);
        // 기존 이미지 삭제
    }

    public List<UserGroupResponse> getGroupList(Long userId) {
        List<UserGroup> userGroups = userGroupRepository.findAllByUserIdWithGroup(userId);
        return userGroups.stream()
            .map(UserGroupResponse::from)
            .toList();
    }

    public GroupDetailResponse getGroupDetail(Long userId, Long groupId) {
        Optional<UserGroup> optionalUserGroup = userGroupRepository.findByUserIdAndGroupId(userId, groupId);
        Group group = findGroupBy(groupId);

        boolean isGroupLeader = optionalUserGroup
            .filter(userGroup -> userGroup.getRole() == GroupRole.LEADER)
            .isPresent();
        return GroupDetailResponse.of(group, isGroupLeader);
    }

    @Transactional
    public Long attendGroup(Long userId, Long groupId) {
        if (userGroupRepository.existsByUserIdAndGroupId(userId, groupId)) {
            throw new CustomException(DUPLICATE_GROUP_MEMBER);
        }

        User user = findUserBy(userId);
        Group group = findGroupBy(groupId);

        UserGroup groupParticipant = UserGroup.createParticipant(snowflake.nextId(), user, group);
        userGroupRepository.save(groupParticipant);

        createDiningParticipantToAttendanceVote(user, groupId);

        groupRepository.increaseTotalMembers(groupId);
        return groupId;
    }

    private void createDiningParticipantToAttendanceVote(User user, Long groupId) {
        List<Dining> diningList = diningRepository.findAllByGroupIdAndDiningStatus(groupId, DiningStatus.ATTENDANCE_VOTING);
        List<DiningParticipant> diningParticipants = diningList.stream()
            .map(dining -> new DiningParticipant(snowflake.nextId(), dining, user, AttendanceVoteStatus.PENDING))
            .toList();
        diningParticipantRepository.saveAll(diningParticipants);
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
