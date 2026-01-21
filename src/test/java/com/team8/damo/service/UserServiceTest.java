package com.team8.damo.service;

import com.team8.damo.controller.response.UserProfileResponse;
import com.team8.damo.entity.*;
import com.team8.damo.entity.enumeration.*;
import com.team8.damo.exception.CustomException;
import com.team8.damo.fixture.CategoryFixture;
import com.team8.damo.fixture.UserFixture;
import com.team8.damo.repository.*;
import com.team8.damo.service.request.UserBasicUpdateServiceRequest;
import com.team8.damo.service.request.UserCharacteristicsCreateServiceRequest;
import com.team8.damo.util.Snowflake;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.team8.damo.exception.errorcode.ErrorCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;


@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AllergyCategoryRepository allergyCategoryRepository;

    @Mock
    private LikeFoodCategoryRepository likeFoodCategoryRepository;

    @Mock
    private LikeIngredientCategoryRepository likeIngredientCategoryRepository;

    @Mock
    private UserAllergyRepository userAllergyRepository;

    @Mock
    private UserLikeFoodRepository userLikeFoodRepository;

    @Mock
    private UserLikeIngredientRepository userLikeIngredientRepository;

    @Mock
    private Snowflake snowflake;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("사용자 기본 정보를 성공적으로 업데이트한다.")
    void updateUserBasic_success() {
        // given
        Long userId = 1L;
        String nickname = "맛집탐험가";
        Gender gender = Gender.MALE;
        AgeGroup ageGroup = AgeGroup.TWENTIES;

        UserBasicUpdateServiceRequest request = new UserBasicUpdateServiceRequest(
            nickname, gender, ageGroup
        );

        User user = UserFixture.create(userId);

        given(userRepository.existsByNickname(nickname)).willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        userService.updateUserBasic(userId, request);

        // then
        assertThat(user.getNickname()).isEqualTo(nickname);
        assertThat(user.getGender()).isEqualTo(gender);
        assertThat(user.getAgeGroup()).isEqualTo(ageGroup);

        then(userRepository).should().existsByNickname(nickname);
        then(userRepository).should().findById(userId);
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임으로 업데이트할 수 없다.")
    void updateUserBasic_duplicateNickname() {
        // given
        Long userId = 1L;
        String duplicateNickname = "중복닉네임";

        UserBasicUpdateServiceRequest request = new UserBasicUpdateServiceRequest(
            duplicateNickname, Gender.MALE, AgeGroup.TWENTIES
        );

        given(userRepository.existsByNickname(duplicateNickname)).willReturn(true);

        // when // then
        assertThatThrownBy(() -> userService.updateUserBasic(userId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DUPLICATE_NICKNAME);

        then(userRepository).should().existsByNickname(duplicateNickname);
        then(userRepository).should(never()).findById(userId);
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 기본 정보를 업데이트할 수 없다.")
    void updateUserBasic_userNotFound() {
        // given
        Long userId = 999L;
        String nickname = "새닉네임";

        UserBasicUpdateServiceRequest request = new UserBasicUpdateServiceRequest(
            nickname, Gender.FEMALE, AgeGroup.THIRTIES
        );

        given(userRepository.existsByNickname(nickname)).willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> userService.updateUserBasic(userId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", USER_NOT_FOUND);

        then(userRepository).should().existsByNickname(nickname);
        then(userRepository).should().findById(userId);
    }

    @Test
    @DisplayName("사용자 개인 특성을 성공적으로 등록한다.")
    void createCharacteristics_success() {
        // given
        Long userId = 1L;
        List<Integer> allergyIds = List.of(1, 2);
        List<Integer> likeFoodIds = List.of(1, 3);
        List<Integer> likeIngredientIds = List.of(2, 4);
        String otherCharacteristics = "매운 음식을 좋아합니다";

        UserCharacteristicsCreateServiceRequest request = new UserCharacteristicsCreateServiceRequest(
            allergyIds, likeFoodIds, likeIngredientIds, otherCharacteristics
        );

        User user = UserFixture.create(userId);
        AllergyCategory allergy1 = new AllergyCategory(null);
        AllergyCategory allergy2 = new AllergyCategory(null);
        LikeFoodCategory food1 = new LikeFoodCategory(null);
        LikeFoodCategory food2 = new LikeFoodCategory(null);
        LikeIngredientCategory ingredient1 = new LikeIngredientCategory(null);
        LikeIngredientCategory ingredient2 = new LikeIngredientCategory(null);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(allergyCategoryRepository.findAllById(allergyIds)).willReturn(List.of(allergy1, allergy2));
        given(likeFoodCategoryRepository.findAllById(likeFoodIds)).willReturn(List.of(food1, food2));
        given(likeIngredientCategoryRepository.findAllById(likeIngredientIds)).willReturn(List.of(ingredient1, ingredient2));
        given(snowflake.nextId()).willReturn(100L, 101L, 102L, 103L, 104L, 105L);

        // when
        userService.createCharacteristics(userId, request);

        // then
        assertThat(user.getOtherCharacteristics()).isEqualTo(otherCharacteristics);
        assertThat(user.getOnboardingStep()).isEqualTo(OnboardingStep.DONE);

        then(userAllergyRepository).should().saveAll(anyList());
        then(userLikeFoodRepository).should().saveAll(anyList());
        then(userLikeIngredientRepository).should().saveAll(anyList());
    }

    @Test
    @DisplayName("모든 리스트가 비어있어도 특성 등록에 성공한다.")
    void createCharacteristics_emptyLists() {
        // given
        Long userId = 1L;

        UserCharacteristicsCreateServiceRequest request = new UserCharacteristicsCreateServiceRequest(
            List.of(), List.of(), List.of(), null
        );

        User user = UserFixture.create(userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        userService.createCharacteristics(userId, request);

        // then
        assertThat(user.getOtherCharacteristics()).isNull();
        assertThat(user.getOnboardingStep()).isEqualTo(OnboardingStep.DONE);

        then(userAllergyRepository).should(never()).saveAll(anyList());
        then(userLikeFoodRepository).should(never()).saveAll(anyList());
        then(userLikeIngredientRepository).should(never()).saveAll(anyList());
    }

    @Test
    @DisplayName("알레르기 카테고리 ID가 중복되면 예외가 발생한다.")
    void createCharacteristics_duplicateAllergyIds() {
        // given
        Long userId = 1L;
        List<Integer> duplicateAllergyIds = List.of(1, 1, 2);

        UserCharacteristicsCreateServiceRequest request = new UserCharacteristicsCreateServiceRequest(
            duplicateAllergyIds, List.of(), List.of(), null
        );

        // when // then
        assertThatThrownBy(() -> userService.createCharacteristics(userId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DUPLICATE_ALLERGY_CATEGORY);

        then(userRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("선호 음식 카테고리 ID가 중복되면 예외가 발생한다.")
    void createCharacteristics_duplicateLikeFoodIds() {
        // given
        Long userId = 1L;
        List<Integer> duplicateFoodIds = List.of(1, 2, 2);

        UserCharacteristicsCreateServiceRequest request = new UserCharacteristicsCreateServiceRequest(
            List.of(), duplicateFoodIds, List.of(), null
        );

        // when // then
        assertThatThrownBy(() -> userService.createCharacteristics(userId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DUPLICATE_LIKE_FOOD_CATEGORY);

        then(userRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("선호 재료 카테고리 ID가 중복되면 예외가 발생한다.")
    void createCharacteristics_duplicateLikeIngredientIds() {
        // given
        Long userId = 1L;
        List<Integer> duplicateIngredientIds = List.of(3, 3, 4);

        UserCharacteristicsCreateServiceRequest request = new UserCharacteristicsCreateServiceRequest(
            List.of(), List.of(), duplicateIngredientIds, null
        );

        // when // then
        assertThatThrownBy(() -> userService.createCharacteristics(userId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DUPLICATE_LIKE_INGREDIENT_CATEGORY);

        then(userRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("존재하지 않는 알레르기 카테고리 ID면 예외가 발생한다.")
    void createCharacteristics_invalidAllergyCategory() {
        // given
        Long userId = 1L;
        List<Integer> allergyIds = List.of(1, 999);

        UserCharacteristicsCreateServiceRequest request = new UserCharacteristicsCreateServiceRequest(
            allergyIds, List.of(), List.of(), null
        );

        User user = UserFixture.create(userId);
        AllergyCategory allergy1 = new AllergyCategory(null);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(allergyCategoryRepository.findAllById(allergyIds)).willReturn(List.of(allergy1));

        // when // then
        assertThatThrownBy(() -> userService.createCharacteristics(userId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", INVALID_CATEGORY);
    }

    @Test
    @DisplayName("존재하지 않는 선호 음식 카테고리 ID면 예외가 발생한다.")
    void createCharacteristics_invalidLikeFoodCategory() {
        // given
        Long userId = 1L;
        List<Integer> likeFoodIds = List.of(1, 999);

        UserCharacteristicsCreateServiceRequest request = new UserCharacteristicsCreateServiceRequest(
            List.of(), likeFoodIds, List.of(), null
        );

        User user = UserFixture.create(userId);
        LikeFoodCategory food1 = new LikeFoodCategory(null);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(likeFoodCategoryRepository.findAllById(likeFoodIds)).willReturn(List.of(food1));

        // when // then
        assertThatThrownBy(() -> userService.createCharacteristics(userId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", INVALID_CATEGORY);
    }

    @Test
    @DisplayName("존재하지 않는 선호 재료 카테고리 ID면 예외가 발생한다.")
    void createCharacteristics_invalidLikeIngredientCategory() {
        // given
        Long userId = 1L;
        List<Integer> likeIngredientIds = List.of(1, 999);

        UserCharacteristicsCreateServiceRequest request = new UserCharacteristicsCreateServiceRequest(
            List.of(), List.of(), likeIngredientIds, null
        );

        User user = UserFixture.create(userId);
        LikeIngredientCategory ingredient1 = new LikeIngredientCategory(null);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(likeIngredientCategoryRepository.findAllById(likeIngredientIds)).willReturn(List.of(ingredient1));

        // when // then
        assertThatThrownBy(() -> userService.createCharacteristics(userId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", INVALID_CATEGORY);
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 특성을 등록할 수 없다.")
    void createCharacteristics_userNotFound() {
        // given
        Long userId = 999L;

        UserCharacteristicsCreateServiceRequest request = new UserCharacteristicsCreateServiceRequest(
            List.of(), List.of(), List.of(), null
        );

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> userService.createCharacteristics(userId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", USER_NOT_FOUND);
    }

    @Test
    @DisplayName("사용자 프로필을 성공적으로 조회한다.")
    void getUserProfile_success() {
        // given
        Long userId = 1L;
        User user = UserFixture.create(userId);
        user.updateBasic("맛집탐험가", Gender.MALE, AgeGroup.TWENTIES);

        AllergyCategory allergy1 = CategoryFixture.createAllergyCategory(1, AllergyType.SHRIMP);
        AllergyCategory allergy2 = CategoryFixture.createAllergyCategory(2, AllergyType.CRAB);
        LikeFoodCategory food1 = CategoryFixture.createLikeFoodCategory(1, FoodType.KOREAN);
        LikeFoodCategory food2 = CategoryFixture.createLikeFoodCategory(2, FoodType.CHINESE);
        LikeIngredientCategory ingredient1 = CategoryFixture.createLikeIngredientCategory(1, IngredientType.MEAT);

        List<UserAllergy> userAllergies = List.of(
            new UserAllergy(100L, user, allergy1),
            new UserAllergy(101L, user, allergy2)
        );
        List<UserLikeFood> userLikeFoods = List.of(
            new UserLikeFood(200L, user, food1),
            new UserLikeFood(201L, user, food2)
        );
        List<UserLikeIngredient> userLikeIngredients = List.of(
            new UserLikeIngredient(300L, user, ingredient1)
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userAllergyRepository.findByUserIdWithCategory(userId)).willReturn(userAllergies);
        given(userLikeFoodRepository.findByUserIdWithCategory(userId)).willReturn(userLikeFoods);
        given(userLikeIngredientRepository.findByUserIdWithCategory(userId)).willReturn(userLikeIngredients);

        // when
        UserProfileResponse response = userService.getUserProfile(userId);

        // then
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getNickname()).isEqualTo("맛집탐험가");
        assertThat(response.getAllergies())
            .extracting("id", "category")
            .containsExactly(
                tuple(1, "새우"),
                tuple(2, "게")
            );
        assertThat(response.getLikeFoods())
            .extracting("id", "category")
            .containsExactly(
                tuple(1, "한식"),
                tuple(2, "중식")
            );
        assertThat(response.getLikeIngredients())
            .extracting("id", "category")
            .containsExactly(
                tuple(1, "육류")
            );

        then(userRepository).should().findById(userId);
        then(userAllergyRepository).should().findByUserIdWithCategory(userId);
        then(userLikeFoodRepository).should().findByUserIdWithCategory(userId);
        then(userLikeIngredientRepository).should().findByUserIdWithCategory(userId);
    }

    @Test
    @DisplayName("카테고리가 모두 비어있어도 프로필 조회에 성공한다.")
    void getUserProfile_emptyCategories() {
        // given
        Long userId = 1L;
        User user = UserFixture.create(userId);
        user.updateBasic("새회원", Gender.FEMALE, AgeGroup.THIRTIES);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userAllergyRepository.findByUserIdWithCategory(userId)).willReturn(List.of());
        given(userLikeFoodRepository.findByUserIdWithCategory(userId)).willReturn(List.of());
        given(userLikeIngredientRepository.findByUserIdWithCategory(userId)).willReturn(List.of());

        // when
        UserProfileResponse response = userService.getUserProfile(userId);

        // then
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getNickname()).isEqualTo("새회원");
        assertThat(response.getAllergies()).isEmpty();
        assertThat(response.getLikeFoods()).isEmpty();
        assertThat(response.getLikeIngredients()).isEmpty();

        then(userRepository).should().findById(userId);
        then(userAllergyRepository).should().findByUserIdWithCategory(userId);
        then(userLikeFoodRepository).should().findByUserIdWithCategory(userId);
        then(userLikeIngredientRepository).should().findByUserIdWithCategory(userId);
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 프로필을 조회할 수 없다.")
    void getUserProfile_userNotFound() {
        // given
        Long userId = 999L;

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> userService.getUserProfile(userId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", USER_NOT_FOUND);

        then(userRepository).should().findById(userId);
        then(userAllergyRepository).should(never()).findByUserIdWithCategory(any());
        then(userLikeFoodRepository).should(never()).findByUserIdWithCategory(any());
        then(userLikeIngredientRepository).should(never()).findByUserIdWithCategory(any());
    }
}
