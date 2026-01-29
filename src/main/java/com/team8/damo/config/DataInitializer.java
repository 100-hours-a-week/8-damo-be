package com.team8.damo.config;

import com.team8.damo.entity.*;
import com.team8.damo.entity.enumeration.*;
import com.team8.damo.repository.*;
import com.team8.damo.util.Snowflake;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final int USER_COUNT = 20;
    private static final int GROUP_COUNT = 5;
    private static final int MEMBERS_PER_GROUP = 10;
    private static final int DINING_PER_STATUS = 2;
    private static final int RESTAURANTS_PER_DINING = 5;

    private final AllergyCategoryRepository allergyCategoryRepository;
    private final LikeFoodCategoryRepository likeFoodCategoryRepository;
    private final LikeIngredientCategoryRepository likeIngredientCategoryRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final UserGroupRepository userGroupRepository;
    private final DiningRepository diningRepository;
    private final DiningParticipantRepository diningParticipantRepository;
    private final RecommendRestaurantRepository recommendRestaurantRepository;
    private final RecommendRestaurantVoteRepository recommendRestaurantVoteRepository;
    private final Snowflake snowflake;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initAllergyCategoriesIfEmpty();
        initLikeFoodCategoriesIfEmpty();
        initLikeIngredientCategoriesIfEmpty();
        initTestDataIfEmpty();
    }

    private void initAllergyCategoriesIfEmpty() {
        if (allergyCategoryRepository.count() > 0) {
            return;
        }
        List<AllergyCategory> categories = Arrays.stream(AllergyType.values())
            .map(AllergyCategory::new)
            .toList();
        allergyCategoryRepository.saveAll(categories);
    }

    private void initLikeFoodCategoriesIfEmpty() {
        if (likeFoodCategoryRepository.count() > 0) {
            return;
        }
        List<LikeFoodCategory> categories = Arrays.stream(FoodType.values())
            .map(LikeFoodCategory::new)
            .toList();
        likeFoodCategoryRepository.saveAll(categories);
    }

    private void initLikeIngredientCategoriesIfEmpty() {
        if (likeIngredientCategoryRepository.count() > 0) {
            return;
        }
        List<LikeIngredientCategory> categories = Arrays.stream(IngredientType.values())
            .map(LikeIngredientCategory::new)
            .toList();
        likeIngredientCategoryRepository.saveAll(categories);
    }

    private void initTestDataIfEmpty() {
        if (userRepository.count() > 0) {
            return;
        }

        List<User> users = createUsers();
        List<Group> groups = createGroups();
        createUserGroups(users, groups);
        createDinings(users, groups);

        log.info("Test data initialized: {} users, {} groups", users.size(), groups.size());
    }

    private List<User> createUsers() {
        List<User> users = new ArrayList<>();

        // User 1 with specific email
        User user1 = new User(1L, "kthink03@naver.com", 1000001L);
        user1.updateNickname("사용자1");
        user1.updateOnboardingStep(OnboardingStep.BASIC);
        users.add(user1);

        // Users 2-20
        for (int i = 2; i <= USER_COUNT; i++) {
            User user = new User((long) i, "user" + i + "@test.com", 1000000L + i);
            user.updateNickname("사용자" + i);
            user.updateOnboardingStep(OnboardingStep.DONE);
            users.add(user);
        }

        return userRepository.saveAll(users);
    }

    private List<Group> createGroups() {
        List<Group> groups = new ArrayList<>();

        for (int i = 1; i <= GROUP_COUNT; i++) {
            Group group = Group.builder()
                .id((long) i)
                .name("테스트 그룹 " + i)
                .introduction("테스트 그룹 " + i + " 소개")
                .latitude(37.5665 + (i * 0.01))
                .longitude(126.9780 + (i * 0.01))
                .build();
            groups.add(group);
        }

        return groupRepository.saveAll(groups);
    }

    private void createUserGroups(List<User> users, List<Group> groups) {
        List<UserGroup> userGroups = new ArrayList<>();
        User user1 = users.get(0);

        for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
            Group group = groups.get(groupIndex);

            // Leader: different user for each group (users 2-6 become leaders)
            User leader = users.get(groupIndex + 1);
            userGroups.add(UserGroup.createLeader(snowflake.nextId(), leader, group));

            // User1 always participates in all groups
            userGroups.add(UserGroup.createParticipant(snowflake.nextId(), user1, group));

            // Add 8 more participants (total 10 members per group including leader)
            int memberCount = 2; // leader + user1
            int userIndex = 0;

            while (memberCount < MEMBERS_PER_GROUP && userIndex < users.size()) {
                User user = users.get(userIndex);
                // Skip if already added (leader or user1)
                if (!user.equals(leader) && !user.equals(user1)) {
                    userGroups.add(UserGroup.createParticipant(snowflake.nextId(), user, group));
                    memberCount++;
                }
                userIndex++;
            }

            // Update group's totalMembers
            for (int j = 1; j < MEMBERS_PER_GROUP; j++) {
                group.incrementTotalMembers();
            }
        }

        userGroupRepository.saveAll(userGroups);
    }

    private void createDinings(List<User> users, List<Group> groups) {
        User user1 = users.get(0);
        LocalDateTime baseDate = LocalDateTime.now().plusDays(30);

        for (Group group : groups) {
            List<User> groupMembers = getGroupMembers(group, users);

            // ATTENDANCE_VOTING dinings (2개)
            for (int i = 0; i < DINING_PER_STATUS; i++) {
                createAttendanceVotingDining(group, groupMembers, user1, baseDate.plusDays(i));
            }

            // RESTAURANT_VOTING dinings (2개)
            for (int i = 0; i < DINING_PER_STATUS; i++) {
                createRestaurantVotingDining(group, groupMembers, user1, baseDate.plusDays(10 + i));
            }

            // CONFIRMED dinings (2개)
            for (int i = 0; i < DINING_PER_STATUS; i++) {
                createConfirmedDining(group, groupMembers, baseDate.plusDays(20 + i));
            }
        }
    }

    private List<User> getGroupMembers(Group group, List<User> allUsers) {
        // Get first 10 users based on group creation logic
        List<User> members = new ArrayList<>();
        User user1 = allUsers.get(0);
        int groupIndex = (int) (group.getId() % GROUP_COUNT);

        // Leader
        User leader = allUsers.get((groupIndex % (allUsers.size() - 1)) + 1);
        members.add(leader);

        // User1
        members.add(user1);

        // Other participants
        for (int i = 0; i < allUsers.size() && members.size() < MEMBERS_PER_GROUP; i++) {
            User user = allUsers.get(i);
            if (!members.contains(user)) {
                members.add(user);
            }
        }

        return members.subList(0, Math.min(members.size(), MEMBERS_PER_GROUP));
    }

    private void createAttendanceVotingDining(Group group, List<User> members, User user1, LocalDateTime diningDate) {
        Dining dining = Dining.builder()
            .id(snowflake.nextId())
            .group(group)
            .diningDate(diningDate)
            .voteDueDate(diningDate.minusDays(3))
            .budget(50000)
            .diningStatus(DiningStatus.ATTENDANCE_VOTING)
            .build();
        diningRepository.save(dining);

        List<DiningParticipant> participants = new ArrayList<>();
        int attendCount = 0;
        int nonAttendCount = 0;

        for (User member : members) {
            AttendanceVoteStatus status;

            if (member.equals(user1)) {
                // User1 is PENDING
                status = AttendanceVoteStatus.PENDING;
            } else if (nonAttendCount < 2) {
                // First 2 others vote NON_ATTEND
                status = AttendanceVoteStatus.NON_ATTEND;
                nonAttendCount++;
            } else {
                // Rest vote ATTEND (7 users)
                status = AttendanceVoteStatus.ATTEND;
                attendCount++;
            }

            DiningParticipant participant = DiningParticipant.builder()
                .id(snowflake.nextId())
                .dining(dining)
                .user(member)
                .attendanceVoteStatus(status)
                .build();
            participants.add(participant);
        }

        diningParticipantRepository.saveAll(participants);

        // Update attendanceVoteDoneCount (9 voted: 7 ATTEND + 2 NON_ATTEND)
        diningRepository.setAttendanceVoteDoneCount(dining.getId(), 9);
    }

    private void createRestaurantVotingDining(Group group, List<User> members, User user1, LocalDateTime diningDate) {
        Dining dining = Dining.builder()
            .id(snowflake.nextId())
            .group(group)
            .diningDate(diningDate)
            .voteDueDate(diningDate.minusDays(3))
            .budget(50000)
            .diningStatus(DiningStatus.RESTAURANT_VOTING)
            .build();
        dining.changeRecommendationCount(1);
        diningRepository.save(dining);

        // All members vote ATTEND
        List<DiningParticipant> participants = new ArrayList<>();
        for (User member : members) {
            DiningParticipant participant = DiningParticipant.builder()
                .id(snowflake.nextId())
                .dining(dining)
                .user(member)
                .attendanceVoteStatus(AttendanceVoteStatus.ATTEND)
                .build();
            participants.add(participant);
        }
        diningParticipantRepository.saveAll(participants);

        // Update attendanceVoteDoneCount (all 10 voted)
        diningRepository.setAttendanceVoteDoneCount(dining.getId(), MEMBERS_PER_GROUP);

        List<String> recommendationRestaurantIds = List.of(
            "6976b54010e1fa815903d4ce",
            "6976b57f10e1fa815903d4cf",
            "6976b58610e1fa815903d4d0",
            "6976b8b9fb8d6fe1764695b6",
            "6976b8bafb8d6fe1764695b7"
        );

        // Create 5 recommended restaurants
        List<RecommendRestaurant> restaurants = new ArrayList<>();
        for (int i = 1; i <= RESTAURANTS_PER_DINING; i++) {
            RecommendRestaurant restaurant = RecommendRestaurant.builder()
                .id(snowflake.nextId())
                .dining(dining)
                .restaurantId(recommendationRestaurantIds.get(i - 1))
                .confirmedStatus(false)
                .likeCount(0)
                .dislikeCount(0)
                .point(100 - (i * 10))
                .reasoningDescription("추천 이유 " + i + ": 맛있고 분위기 좋은 식당입니다.")
                .recommendationCount(1)
                .build();
            restaurants.add(restaurant);
        }
        recommendRestaurantRepository.saveAll(restaurants);

        // All users except user1 vote on restaurants
        List<RecommendRestaurantVote> votes = new ArrayList<>();
        for (User member : members) {
            if (member.equals(user1)) {
                continue; // User1 doesn't vote
            }

            for (int i = 0; i < restaurants.size(); i++) {
                RecommendRestaurant restaurant = restaurants.get(i);
                // Alternate between LIKE and DISLIKE
                RestaurantVoteStatus voteStatus = (i % 2 == 0) ? RestaurantVoteStatus.LIKE : RestaurantVoteStatus.DISLIKE;

                RecommendRestaurantVote vote = RecommendRestaurantVote.builder()
                    .id(snowflake.nextId())
                    .user(member)
                    .recommendRestaurant(restaurant)
                    .status(voteStatus)
                    .build();
                votes.add(vote);
            }
        }
        recommendRestaurantVoteRepository.saveAll(votes);

        // Update like/dislike counts
        for (int i = 0; i < restaurants.size(); i++) {
            RecommendRestaurant restaurant = restaurants.get(i);
            int likeCount = (i % 2 == 0) ? 9 : 0;  // 9 users voted (excluding user1)
            int dislikeCount = (i % 2 == 0) ? 0 : 9;
            recommendRestaurantRepository.setVoteCounts(restaurant.getId(), likeCount, dislikeCount);
        }
    }

    private void createConfirmedDining(Group group, List<User> members, LocalDateTime diningDate) {
        Dining dining = Dining.builder()
            .id(snowflake.nextId())
            .group(group)
            .diningDate(diningDate)
            .voteDueDate(diningDate.minusDays(3))
            .budget(50000)
            .diningStatus(DiningStatus.CONFIRMED)
            .build();
        diningRepository.save(dining);

        // All members voted ATTEND
        List<DiningParticipant> participants = new ArrayList<>();
        for (User member : members) {
            DiningParticipant participant = DiningParticipant.builder()
                .id(snowflake.nextId())
                .dining(dining)
                .user(member)
                .attendanceVoteStatus(AttendanceVoteStatus.ATTEND)
                .build();
            participants.add(participant);
        }
        diningParticipantRepository.saveAll(participants);

        // Update attendanceVoteDoneCount
        diningRepository.setAttendanceVoteDoneCount(dining.getId(), MEMBERS_PER_GROUP);

        // Create 5 recommended restaurants, first one is confirmed
        List<RecommendRestaurant> restaurants = new ArrayList<>();
        for (int i = 1; i <= RESTAURANTS_PER_DINING; i++) {
            RecommendRestaurant restaurant = RecommendRestaurant.builder()
                .id(snowflake.nextId())
                .dining(dining)
                .restaurantId("restaurant_" + dining.getId() + "_" + i)
                .confirmedStatus(i == 1) // First restaurant is confirmed
                .likeCount(i == 1 ? 8 : 2) // Confirmed has most likes
                .dislikeCount(i == 1 ? 1 : 5)
                .point(100 - (i * 10))
                .reasoningDescription("추천 이유 " + i + ": 맛있고 분위기 좋은 식당입니다.")
                .build();
            restaurants.add(restaurant);
        }
        recommendRestaurantRepository.saveAll(restaurants);
    }
}
