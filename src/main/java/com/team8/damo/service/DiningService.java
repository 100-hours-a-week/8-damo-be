package com.team8.damo.service;

import com.team8.damo.entity.*;
import com.team8.damo.entity.enumeration.DiningStatus;
import com.team8.damo.entity.enumeration.GroupRole;
import com.team8.damo.entity.enumeration.VotingStatus;
import com.team8.damo.exception.CustomException;
import com.team8.damo.repository.*;
import com.team8.damo.service.request.DiningCreateServiceRequest;
import com.team8.damo.service.response.DiningResponse;
import com.team8.damo.util.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.team8.damo.exception.errorcode.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiningService {

    private static final int MAX_INCOMPLETE_DINING_COUNT = 3;

    private final Snowflake snowflake;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;
    private final DiningRepository diningRepository;
    private final DiningParticipantRepository diningParticipantRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long createDining(Long userId, Long groupId, DiningCreateServiceRequest request, LocalDateTime currentDataTime) {
        User user = findUserBy(userId);
        Group group = findGroupBy(groupId);

        validateGroupLeader(userId, groupId);
        validateDiningDate(request.diningDate(), currentDataTime);
        validateVoteDueDate(request.voteDueDate(), request.diningDate());
        validateDiningLimit(groupId);

        Dining dining = request.toEntity(snowflake.nextId(), group);
        diningRepository.save(dining);

        List<DiningParticipant> participants = createParticipantsForGroupMembers(groupId, dining);
        diningParticipantRepository.saveAll(participants);

        return dining.getId();
    }

    private List<DiningParticipant> createParticipantsForGroupMembers(Long groupId, Dining dining) {
        List<UserGroup> groupMembers = userGroupRepository.findAllByGroupIdWithUser(groupId);

        return groupMembers.stream()
            .map(userGroup -> DiningParticipant.createPendingParticipant(
                snowflake.nextId(),
                dining,
                userGroup.getUser()
            ))
            .toList();
    }

    public List<DiningResponse> getDiningList(Long userId, Long groupId, DiningStatus status) {
        if (isNotGroupMember(userId, groupId)) {
            throw new CustomException(USER_NOT_GROUP_MEMBER);
        }

        List<Dining> dinings = diningRepository.findAllByGroupIdAndDiningStatus(groupId, status);
        List<Long> diningIds = dinings.stream()
            .map(Dining::getId)
            .toList();

        Map<Long, Long> countMap = createAttendCountingMap(diningIds, VotingStatus.ATTEND);

        return dinings.stream()
            .map(dining -> DiningResponse.of(dining, countMap.getOrDefault(dining.getId(), 0L)))
            .toList();
    }

    @Transactional
    public VotingStatus voteAttendance(Long userId, Long groupId, Long diningId, VotingStatus votingStatus) {
        Dining dining = findDiningBy(diningId);

        if (dining.getDiningStatus().isNotAttendanceVoting()) {
            throw new CustomException(ATTENDANCE_VOTING_CLOSED);
        }

        DiningParticipant participant = findParticipantBy(diningId, userId);

        if (participant.getVotingStatus().isNotPending()) {
            throw new CustomException(ATTENDANCE_VOTE_ALREADY_COMPLETED);
        }

        participant.updateVotingStatus(votingStatus);
        int votedCount = diningRepository.increaseAttendanceVoteDoneCount(diningId);

        checkAllParticipantsVoted(diningId, dining, votedCount);
        // 투표 진척도를 그룹원들에게 전송하는 sse 구현

        return participant.getVotingStatus();
    }

    private void checkAllParticipantsVoted(Long diningId, Dining dining, int votedCount) {
        int totalParticipants = diningParticipantRepository.countByDiningId(diningId);

        // 모두 참석 투표를 완료하면 장소 추천 진행
        if (votedCount >= totalParticipants) {
            // AI 추천 요청
            // aiRecommendationService.requestRestaurantRecommendation(event.diningId(), event.groupId());

            dining.changeStatus(DiningStatus.RESTAURANT_VOTING);
        }
    }

    private void validateGroupLeader(Long userId, Long groupId) {
        boolean isGroupLeader = userGroupRepository.existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER);
        if (!isGroupLeader) {
            throw new CustomException(ONLY_GROUP_LEADER_ALLOWED);
        }
    }

    private void validateDiningDate(LocalDateTime diningDate, LocalDateTime now) {
        if (!diningDate.isAfter(now)) {
            throw new CustomException(DINING_DATE_MUST_BE_AFTER_NOW);
        }
    }

    private void validateVoteDueDate(LocalDateTime voteDueDate, LocalDateTime diningDate) {
        if (!voteDueDate.isBefore(diningDate)) {
            throw new CustomException(VOTE_DUE_DATE_MUST_BE_BEFORE_DINING_DATE);
        }
    }

    private void validateDiningLimit(Long groupId) {
        int incompleteDiningCount = diningRepository.countByGroupIdAndDiningStatusNot(groupId, DiningStatus.COMPLETE);
        if (incompleteDiningCount >= MAX_INCOMPLETE_DINING_COUNT) {
            throw new CustomException(DINING_LIMIT_EXCEEDED);
        }
    }

    private Map<Long, Long> createAttendCountingMap(List<Long> diningIds, VotingStatus votingStatus) {
        return diningParticipantRepository.findByDiningIdInAndVotingStatus(diningIds, votingStatus)
            .stream()
            .collect(Collectors.groupingBy(
                dp -> dp.getDining().getId(),
                Collectors.counting()
            ));
    }

    private User findUserBy(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }

    private Group findGroupBy(Long groupId) {
        return groupRepository.findById(groupId)
            .orElseThrow(() -> new CustomException(GROUP_NOT_FOUND));
    }

    private Dining findDiningBy(Long diningId) {
        return diningRepository.findById(diningId)
            .orElseThrow(() -> new CustomException(DINING_NOT_FOUND));
    }

    private DiningParticipant findParticipantBy(Long diningId, Long userId) {
        return diningParticipantRepository.findByDiningIdAndUserId(diningId, userId)
            .orElseThrow(() -> new CustomException(NO_VOTE_PERMISSION));
    }

    private boolean isNotGroupMember(Long userId, Long groupId) {
        return !userGroupRepository.existsByUserIdAndGroupId(userId, groupId);
    }
}
