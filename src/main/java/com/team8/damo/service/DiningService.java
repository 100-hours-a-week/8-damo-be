package com.team8.damo.service;

import com.team8.damo.client.AiService;
import com.team8.damo.entity.Dining;
import com.team8.damo.entity.DiningParticipant;
import com.team8.damo.entity.Group;
import com.team8.damo.entity.RecommendRestaurant;
import com.team8.damo.entity.RecommendRestaurantVote;
import com.team8.damo.entity.Restaurant;
import com.team8.damo.entity.User;
import com.team8.damo.entity.UserGroup;
import com.team8.damo.entity.enumeration.AttendanceVoteStatus;
import com.team8.damo.entity.enumeration.DiningStatus;
import com.team8.damo.entity.enumeration.GroupRole;
import com.team8.damo.entity.enumeration.RestaurantVoteStatus;
import com.team8.damo.event.EventType;
import com.team8.damo.event.handler.CommonEventPublisher;
import com.team8.damo.event.payload.RecommendationEventPayload;
import com.team8.damo.exception.CustomException;
import com.team8.damo.repository.*;
import com.team8.damo.service.request.DiningCreateServiceRequest;
import com.team8.damo.service.request.RestaurantVoteServiceRequest;
import com.team8.damo.service.response.AttendanceVoteDetailResponse;
import com.team8.damo.service.response.DiningDetailResponse;
import com.team8.damo.service.response.DiningParticipantResponse;
import com.team8.damo.service.response.DiningResponse;
import com.team8.damo.service.response.RestaurantVoteDetailResponse;
import com.team8.damo.service.response.DiningConfirmedResponse;
import com.team8.damo.service.response.RestaurantVoteResponse;
import com.team8.damo.event.RestaurantRecommendationEvent;
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

    private static final String VOTE_CANCELLED = "NONE";
    private static final int MAX_INCOMPLETE_DINING_COUNT = 3;

    private final Snowflake snowflake;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;
    private final DiningRepository diningRepository;
    private final DiningParticipantRepository diningParticipantRepository;
    private final RecommendRestaurantRepository recommendRestaurantRepository;
    private final RecommendRestaurantVoteRepository recommendRestaurantVoteRepository;
    private final RestaurantRepository restaurantRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AiService aiService;
    private final CommonEventPublisher commonEventPublisher;

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

        Map<Long, Long> countMap = createAttendCountingMap(diningIds, AttendanceVoteStatus.ATTEND);

        return dinings.stream()
            .map(dining -> DiningResponse.of(dining, countMap.getOrDefault(dining.getId(), 0L)))
            .toList();
    }

    public DiningDetailResponse getDiningDetail(Long userId, Long groupId, Long diningId) {
        UserGroup userGroup = userGroupRepository.findByUserIdAndGroupId(userId, groupId)
            .orElseThrow(() -> new CustomException(USER_NOT_GROUP_MEMBER));

        boolean isGroupLeader = userGroup.getRole() == GroupRole.LEADER;

        Dining dining = findDiningBy(diningId);

        List<DiningParticipant> attendParticipants =
            diningParticipantRepository.findAllByDiningAndAttendanceVoteStatus(
                dining, AttendanceVoteStatus.ATTEND
            );

        List<DiningParticipantResponse> participantResponses = attendParticipants.stream()
            .map(DiningParticipantResponse::from)
            .toList();

        return DiningDetailResponse.of(isGroupLeader, dining, participantResponses);
    }

    public AttendanceVoteDetailResponse getAttendanceVoteDetail(Long userId, Long groupId, Long diningId) {
        if (isNotGroupMember(userId, groupId)) {
            throw new CustomException(USER_NOT_GROUP_MEMBER);
        }
        Dining dining = findDiningBy(diningId);
        DiningParticipant participant = findParticipantBy(diningId, userId);
        return AttendanceVoteDetailResponse.of(participant, dining);
    }

    public List<RestaurantVoteDetailResponse> getRestaurantVoteDetail(Long userId, Long groupId, Long diningId) {
        if (isNotGroupMember(userId, groupId)) {
            throw new CustomException(USER_NOT_GROUP_MEMBER);
        }

        Dining dining = findDiningBy(diningId);

        List<RecommendRestaurant> recommendRestaurants =
            recommendRestaurantRepository.findByDiningIdAndRecommendationCount(
                diningId, dining.getRecommendationCount()
            );

        DiningParticipant participant = findParticipantBy(diningId, userId);
        if (participant.getAttendanceVoteStatus() != AttendanceVoteStatus.ATTEND) {
            throw new CustomException(ONLY_ATTEND_PARTICIPANT_CAN_VOTE);
        }

        return mapToVoteDetailResponses(userId, recommendRestaurants);
    }

    private List<RestaurantVoteDetailResponse> mapToVoteDetailResponses(Long userId, List<RecommendRestaurant> recommendRestaurants) {
        return recommendRestaurants.stream()
            .map(recommendRestaurant -> {
                Restaurant restaurant = restaurantRepository.findById(recommendRestaurant.getRestaurantId())
                    .orElseThrow(() -> new CustomException(RESTAURANT_NOT_FOUND));

                RestaurantVoteStatus userVoteStatus = recommendRestaurantVoteRepository
                    .findByUserIdAndRecommendRestaurantId(userId, recommendRestaurant.getId())
                    .map(RecommendRestaurantVote::getStatus)
                    .orElse(null);

                return RestaurantVoteDetailResponse.of(recommendRestaurant, restaurant, userVoteStatus);
            })
            .toList();
    }

    @Transactional
    public AttendanceVoteStatus voteAttendance(Long userId, Long groupId, Long diningId, AttendanceVoteStatus attendanceVoteStatus) {
        Dining dining = findDiningBy(diningId);

        if (dining.getDiningStatus().isNotAttendanceVoting()) {
            throw new CustomException(ATTENDANCE_VOTING_CLOSED);
        }

        DiningParticipant participant = findParticipantBy(diningId, userId);
        boolean isFirstVote = participant.isFirstAttendanceVote();
        participant.updateVotingStatus(attendanceVoteStatus);

        if (isFirstVote) {
            int votedCount = diningRepository.increaseAttendanceVoteDoneCount(diningId);
            triggerRestaurantRecommendation(groupId, dining, votedCount);
        }

        return participant.getAttendanceVoteStatus();
    }

    private void triggerRestaurantRecommendation(Long groupId, Dining dining, int votedCount) {
        int totalParticipants = diningParticipantRepository.countByDiningId(dining.getId());

        if (votedCount < totalParticipants) return;

        // 모두 참석 투표를 완료하면 AI 장소 추천 요청
        Group group = findGroupBy(groupId);
        List<Long> userIds = createAttendParticipantIds(dining);

        commonEventPublisher.publish(
            EventType.RESTAURANT_RECOMMENDATION,
            RecommendationEventPayload.builder()
                .group(group)
                .dining(dining)
                .userIds(userIds)
                .build()
        );

        dining.startRecommendationPending();
    }

    private List<Long> createAttendParticipantIds(Dining dining) {
        List<DiningParticipant> attendParticipants = diningParticipantRepository.findAllByDiningAndAttendanceVoteStatus(dining, AttendanceVoteStatus.ATTEND);
        return attendParticipants.stream()
            .map(participant -> participant.getUser().getId())
            .toList();
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

    private Map<Long, Long> createAttendCountingMap(List<Long> diningIds, AttendanceVoteStatus attendanceVoteStatus) {
        return diningParticipantRepository.findByDiningIdInAndAttendanceVoteStatus(diningIds, attendanceVoteStatus)
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
            .orElseThrow(() -> new CustomException(DINING_PARTICIPANT_NOT_FOUND));
    }

    private boolean isNotGroupMember(Long userId, Long groupId) {
        return !userGroupRepository.existsByUserIdAndGroupId(userId, groupId);
    }

    @Transactional
    public RestaurantVoteResponse voteRestaurant(
        Long userId, Long groupId, Long diningId,
        Long recommendRestaurantId, RestaurantVoteServiceRequest request
    ) {
        if (isNotGroupMember(userId, groupId)) {
            throw new CustomException(USER_NOT_GROUP_MEMBER);
        }

        Dining dining = findDiningBy(diningId);
        if (dining.getDiningStatus().isNotRestaurantVoting()) {
            throw new CustomException(RESTAURANT_VOTING_CLOSED);
        }

        User user = findUserBy(userId);
        RecommendRestaurant restaurant = findRecommendRestaurantBy(recommendRestaurantId);

        RestaurantVoteStatus newStatus = request.restaurantVoteStatus();

        String resultStatus = recommendRestaurantVoteRepository
            .findByUserIdAndRecommendRestaurantId(userId, recommendRestaurantId)
            .map(previousVote -> handlePreviousVote(previousVote, newStatus, recommendRestaurantId))
            .orElseGet(() -> createNewVote(user, restaurant, newStatus));

        return RestaurantVoteResponse.of(recommendRestaurantId, resultStatus);
    }

    private String handlePreviousVote(
        RecommendRestaurantVote previousVote,
        RestaurantVoteStatus newStatus,
        Long recommendRestaurantId
    ) {
        RestaurantVoteStatus previousStatus = previousVote.getStatus();

        if (previousStatus == newStatus) {
            recommendRestaurantVoteRepository.delete(previousVote);
            previousStatus.decreaseCount(recommendRestaurantId, recommendRestaurantRepository);
            return VOTE_CANCELLED;
        }

        previousStatus.decreaseCount(recommendRestaurantId, recommendRestaurantRepository);
        newStatus.increaseCount(recommendRestaurantId, recommendRestaurantRepository);
        previousVote.changeStatus(newStatus);
        return newStatus.name();
    }

    private String createNewVote(User user, RecommendRestaurant restaurant, RestaurantVoteStatus newStatus) {
        RecommendRestaurantVote newVote = RecommendRestaurantVote.builder()
            .id(snowflake.nextId())
            .user(user)
            .recommendRestaurant(restaurant)
            .status(newStatus)
            .build();
        recommendRestaurantVoteRepository.save(newVote);
        newStatus.increaseCount(restaurant.getId(), recommendRestaurantRepository);
        return newStatus.name();
    }

    private RecommendRestaurant findRecommendRestaurantBy(Long id) {
        return recommendRestaurantRepository.findById(id)
            .orElseThrow(() -> new CustomException(RECOMMEND_RESTAURANT_NOT_FOUND));
    }

    public DiningConfirmedResponse getDiningConfirmed(Long userId, Long groupId, Long diningId) {
        if (isNotGroupMember(userId, groupId)) {
            throw new CustomException(USER_NOT_GROUP_MEMBER);
        }

        Dining dining = findDiningBy(diningId);
        if (dining.isNotRestaurantConfirmed()) {
            throw new CustomException(DINING_NOT_CONFIRMED);
        }

        RecommendRestaurant confirmedRestaurant =
            recommendRestaurantRepository.findConfirmedRecommendRestaurant(
                diningId,
                dining.getRecommendationCount()
            ).orElseThrow(() -> new CustomException(RECOMMEND_RESTAURANT_NOT_FOUND));

        Restaurant restaurant = restaurantRepository.findById(confirmedRestaurant.getRestaurantId())
            .orElseThrow(() -> new CustomException(RESTAURANT_NOT_FOUND));

        return DiningConfirmedResponse.of(confirmedRestaurant, restaurant);
    }

    @Transactional
    public DiningConfirmedResponse confirmDiningRestaurant(
        Long userId, Long groupId, Long diningId, Long recommendRestaurantId
    ) {
        if (isNotGroupLeader(userId, groupId)) {
            throw new CustomException(ONLY_GROUP_LEADER_CAN_CONFIRM);
        }

        Dining dining = findDiningBy(diningId);
        RecommendRestaurant recommendRestaurant = findRecommendRestaurantBy(recommendRestaurantId);

        if (recommendRestaurant.isConfirmed()) {
            throw new CustomException(RECOMMEND_RESTAURANT_ALREADY_CONFIRMED);
        }

        if (recommendRestaurantRepository.existsByDiningIdAndRecommendationCountAndConfirmedStatusTrue(diningId, dining.getRecommendationCount())) {
            throw new CustomException(ANOTHER_RESTAURANT_ALREADY_CONFIRMED);
        }

        recommendRestaurant.confirmed();
        dining.confirmed();

        Restaurant restaurant = restaurantRepository.findById(recommendRestaurant.getRestaurantId())
            .orElseThrow(() -> new CustomException(RESTAURANT_NOT_FOUND));

        Group group = findGroupBy(groupId);
        aiService.sendConfirmRestaurant(group, dining, restaurant.getId(), recommendRestaurant);

        return DiningConfirmedResponse.of(recommendRestaurant, restaurant);
    }

    private boolean isNotGroupLeader(Long userId, Long groupId) {
        return !userGroupRepository.existsByUserIdAndGroupIdAndRole(userId, groupId, GroupRole.LEADER);
    }

    @Transactional
    public List<RestaurantVoteDetailResponse> refreshRecommendRestaurants(
        Long userId, Long groupId, Long diningId
    ) {
        if (isNotGroupLeader(userId, groupId)) {
            throw new CustomException(ONLY_GROUP_LEADER_CAN_REFRESH);
        }

        Dining dining = findDiningBy(diningId);
        if (dining.isNotRestaurantVoting()) {
            throw new CustomException(RECOMMEND_REFRESH_ONLY_IN_RESTAURANT_VOTING);
        }

        Group group = findGroupBy(groupId);

        List<Long> userIds = createAttendParticipantIds(dining);
        List<RecommendRestaurant> recommendRestaurants =
            aiService.recommendationRefreshRestaurant(group, dining, userIds);

        return mapToVoteDetailResponses(userId, recommendRestaurants);
    }
}
