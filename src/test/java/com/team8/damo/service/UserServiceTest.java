package com.team8.damo.service;

import com.team8.damo.client.AiService;
import com.team8.damo.entity.*;
import com.team8.damo.entity.enumeration.*;
import com.team8.damo.event.EventType;
import com.team8.damo.event.handler.CommonEventPublisher;
import com.team8.damo.event.payload.UserPersonaPayload;
import com.team8.damo.exception.CustomException;
import com.team8.damo.fixture.CategoryFixture;
import com.team8.damo.fixture.UserFixture;
import com.team8.damo.kakao.KakaoUtil;
import com.team8.damo.repository.*;
import com.team8.damo.service.request.UserBasicUpdateServiceRequest;
import com.team8.damo.service.request.UserCharacteristicsCreateServiceRequest;
import com.team8.damo.service.request.UserCharacteristicsUpdateServiceRequest;
import com.team8.damo.service.response.UserBasicResponse;
import com.team8.damo.service.response.UserProfileResponse;
import com.team8.damo.util.Snowflake;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.team8.damo.exception.errorcode.ErrorCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

@ActiveProfiles("test")
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

    @Mock
    private AiService aiService;

    @Mock
    private CommonEventPublisher commonEventPublisher;

    @Mock
    private KakaoUtil kakaoUtil;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<List<UserAllergy>> userAllergyCaptor;

    @Captor
    private ArgumentCaptor<List<UserLikeFood>> userLikeFoodCaptor;

    @Captor
    private ArgumentCaptor<List<UserLikeIngredient>> userLikeIngredientCaptor;

    @Captor
    private ArgumentCaptor<List<AllergyCategory>> allergyCategoryCaptor;

    @Captor
    private ArgumentCaptor<List<LikeFoodCategory>> likeFoodCategoryCaptor;

    @Captor
    private ArgumentCaptor<List<LikeIngredientCategory>> likeIngredientCategoryCaptor;

    @Captor
    private ArgumentCaptor<UserPersonaPayload> userPersonaPayloadCaptor;

    // ===== Helper Methods for Category Assertion =====

    private void assertSavedAllergies(List<UserAllergy> saved, AllergyType... expectedTypes) {
        assertThat(saved).hasSize(expectedTypes.length)
            .extracting(ua -> ua.getAllergyCategory().getCategory())
            .containsExactlyInAnyOrder(expectedTypes);
    }

    private void assertSavedLikeFoods(List<UserLikeFood> saved, FoodType... expectedTypes) {
        assertThat(saved).hasSize(expectedTypes.length)
            .extracting(ulf -> ulf.getLikeFoodCategory().getCategory())
            .containsExactlyInAnyOrder(expectedTypes);
    }

    private void assertSavedLikeIngredients(List<UserLikeIngredient> saved, IngredientType... expectedTypes) {
        assertThat(saved).hasSize(expectedTypes.length)
            .extracting(uli -> uli.getLikeIngredientCategory().getCategory())
            .containsExactlyInAnyOrder(expectedTypes);
    }

    private void assertDeletedAllergies(List<AllergyCategory> deleted, AllergyType... expectedTypes) {
        assertThat(deleted).hasSize(expectedTypes.length)
            .extracting(AllergyCategory::getCategory)
            .containsExactlyInAnyOrder(expectedTypes);
    }

    private void assertDeletedLikeFoods(List<LikeFoodCategory> deleted, FoodType... expectedTypes) {
        assertThat(deleted).hasSize(expectedTypes.length)
            .extracting(LikeFoodCategory::getCategory)
            .containsExactlyInAnyOrder(expectedTypes);
    }

    private void assertDeletedLikeIngredients(List<LikeIngredientCategory> deleted, IngredientType... expectedTypes) {
        assertThat(deleted).hasSize(expectedTypes.length)
            .extracting(LikeIngredientCategory::getCategory)
            .containsExactlyInAnyOrder(expectedTypes);
    }

    @Test
    @DisplayName("사용자 기본 정보를 성공적으로 업데이트한다.")
    void updateUserBasic_success() {
        // given
        Long userId = 1L;
        String nickname = "맛집탐험가";
        Gender gender = Gender.MALE;
        AgeGroup ageGroup = AgeGroup.TWENTIES;

        UserBasicUpdateServiceRequest request = new UserBasicUpdateServiceRequest(
            nickname, gender, ageGroup, "users/profile/user-1.png"
        );

        User user = UserFixture.create(userId);

        given(userRepository.existsByNicknameAndIdNot(nickname, userId)).willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        userService.updateUserBasic(userId, request);

        // then
        assertThat(user.getNickname()).isEqualTo(nickname);
        assertThat(user.getGender()).isEqualTo(gender);
        assertThat(user.getAgeGroup()).isEqualTo(ageGroup);

        then(userRepository).should().existsByNicknameAndIdNot(nickname, userId);
        then(userRepository).should().findById(userId);
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임으로 업데이트할 수 없다.")
    void updateUserBasic_duplicateNickname() {
        // given
        Long userId = 1L;
        String duplicateNickname = "중복닉네임";

        UserBasicUpdateServiceRequest request = new UserBasicUpdateServiceRequest(
            duplicateNickname, Gender.MALE, AgeGroup.TWENTIES, null
        );

        given(userRepository.existsByNicknameAndIdNot(duplicateNickname, userId)).willReturn(true);

        // when // then
        assertThatThrownBy(() -> userService.updateUserBasic(userId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DUPLICATE_NICKNAME);

        then(userRepository).should().existsByNicknameAndIdNot(duplicateNickname, userId);
        then(userRepository).should(never()).findById(userId);
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 기본 정보를 업데이트할 수 없다.")
    void updateUserBasic_userNotFound() {
        // given
        Long userId = 999L;
        String nickname = "새닉네임";

        UserBasicUpdateServiceRequest request = new UserBasicUpdateServiceRequest(
            nickname, Gender.FEMALE, AgeGroup.THIRTIES, "users/profile/user-999.png"
        );

        given(userRepository.existsByNicknameAndIdNot(nickname, userId)).willReturn(false);
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> userService.updateUserBasic(userId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", USER_NOT_FOUND);

        then(userRepository).should().existsByNicknameAndIdNot(nickname, userId);
        then(userRepository).should().findById(userId);
    }

    @Test
    @DisplayName("사용자 개인 특성을 성공적으로 등록한다.")
    void createCharacteristics_success() {
        // given
        Long userId = 1L;
        List<AllergyType> allergies = List.of(AllergyType.SHRIMP, AllergyType.CRAB);
        List<FoodType> likeFoods = List.of(FoodType.KOREAN, FoodType.CHINESE);
        List<IngredientType> likeIngredients = List.of(IngredientType.MEAT, IngredientType.SEAFOOD);
        String otherCharacteristics = "매운 음식을 좋아합니다";

        UserCharacteristicsCreateServiceRequest request = new UserCharacteristicsCreateServiceRequest(
            allergies, likeFoods, likeIngredients, otherCharacteristics
        );

        User user = UserFixture.create(userId);
        AllergyCategory allergy1 = new AllergyCategory(AllergyType.SHRIMP);
        AllergyCategory allergy2 = new AllergyCategory(AllergyType.CRAB);
        LikeFoodCategory food1 = new LikeFoodCategory(FoodType.KOREAN);
        LikeFoodCategory food2 = new LikeFoodCategory(FoodType.CHINESE);
        LikeIngredientCategory ingredient1 = new LikeIngredientCategory(IngredientType.MEAT);
        LikeIngredientCategory ingredient2 = new LikeIngredientCategory(IngredientType.SEAFOOD);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(allergyCategoryRepository.findByCategoryIn(allergies)).willReturn(List.of(allergy1, allergy2));
        given(likeFoodCategoryRepository.findByCategoryIn(likeFoods)).willReturn(List.of(food1, food2));
        given(likeIngredientCategoryRepository.findByCategoryIn(likeIngredients)).willReturn(List.of(ingredient1, ingredient2));
        given(snowflake.nextId()).willReturn(100L, 101L, 102L, 103L, 104L, 105L);

        // when
        userService.createCharacteristics(userId, request);

        // then
        assertThat(user.getOtherCharacteristics()).isEqualTo(otherCharacteristics);
        assertThat(user.getOnboardingStep()).isEqualTo(OnboardingStep.DONE);

        then(userAllergyRepository).should().saveAll(userAllergyCaptor.capture());
        then(userLikeFoodRepository).should().saveAll(userLikeFoodCaptor.capture());
        then(userLikeIngredientRepository).should().saveAll(userLikeIngredientCaptor.capture());

        assertSavedAllergies(userAllergyCaptor.getValue(), AllergyType.SHRIMP, AllergyType.CRAB);
        assertSavedLikeFoods(userLikeFoodCaptor.getValue(), FoodType.KOREAN, FoodType.CHINESE);
        assertSavedLikeIngredients(userLikeIngredientCaptor.getValue(), IngredientType.MEAT, IngredientType.SEAFOOD);

        then(commonEventPublisher).should().publish(eq(EventType.USER_PERSONA), userPersonaPayloadCaptor.capture());
        UserPersonaPayload capturedPayload = userPersonaPayloadCaptor.getValue();
        assertThat(capturedPayload.user()).isEqualTo(user);
        assertThat(capturedPayload.allergies()).isEqualTo(allergies);
        assertThat(capturedPayload.likeFoods()).isEqualTo(likeFoods);
        assertThat(capturedPayload.likeIngredients()).isEqualTo(likeIngredients);
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

        then(commonEventPublisher).should().publish(eq(EventType.USER_PERSONA), userPersonaPayloadCaptor.capture());
        UserPersonaPayload capturedPayload = userPersonaPayloadCaptor.getValue();
        assertThat(capturedPayload.user()).isEqualTo(user);
        assertThat(capturedPayload.allergies()).isEmpty();
        assertThat(capturedPayload.likeFoods()).isEmpty();
        assertThat(capturedPayload.likeIngredients()).isEmpty();
    }

    @Test
    @DisplayName("알레르기 타입이 중복되면 예외가 발생한다.")
    void createCharacteristics_duplicateAllergies() {
        // given
        Long userId = 1L;
        List<AllergyType> duplicateAllergies = List.of(AllergyType.SHRIMP, AllergyType.SHRIMP, AllergyType.CRAB);

        UserCharacteristicsCreateServiceRequest request = new UserCharacteristicsCreateServiceRequest(
            duplicateAllergies, List.of(), List.of(), null
        );

        // when // then
        assertThatThrownBy(() -> userService.createCharacteristics(userId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DUPLICATE_ALLERGY_CATEGORY);

        then(userRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("선호 음식 타입이 중복되면 예외가 발생한다.")
    void createCharacteristics_duplicateLikeFoods() {
        // given
        Long userId = 1L;
        List<FoodType> duplicateFoods = List.of(FoodType.KOREAN, FoodType.CHINESE, FoodType.CHINESE);

        UserCharacteristicsCreateServiceRequest request = new UserCharacteristicsCreateServiceRequest(
            List.of(), duplicateFoods, List.of(), null
        );

        // when // then
        assertThatThrownBy(() -> userService.createCharacteristics(userId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DUPLICATE_LIKE_FOOD_CATEGORY);

        then(userRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("선호 재료 타입이 중복되면 예외가 발생한다.")
    void createCharacteristics_duplicateLikeIngredients() {
        // given
        Long userId = 1L;
        List<IngredientType> duplicateIngredients = List.of(IngredientType.MEAT, IngredientType.MEAT, IngredientType.SEAFOOD);

        UserCharacteristicsCreateServiceRequest request = new UserCharacteristicsCreateServiceRequest(
            List.of(), List.of(), duplicateIngredients, null
        );

        // when // then
        assertThatThrownBy(() -> userService.createCharacteristics(userId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DUPLICATE_LIKE_INGREDIENT_CATEGORY);

        then(userRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("DB에 존재하지 않는 알레르기 타입이면 예외가 발생한다.")
    void createCharacteristics_invalidAllergyCategory() {
        // given
        Long userId = 1L;
        List<AllergyType> allergies = List.of(AllergyType.SHRIMP, AllergyType.CRAB);

        UserCharacteristicsCreateServiceRequest request = new UserCharacteristicsCreateServiceRequest(
            allergies, List.of(), List.of(), null
        );

        User user = UserFixture.create(userId);
        AllergyCategory allergy1 = new AllergyCategory(AllergyType.SHRIMP);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(allergyCategoryRepository.findByCategoryIn(allergies)).willReturn(List.of(allergy1));

        // when // then
        assertThatThrownBy(() -> userService.createCharacteristics(userId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", INVALID_CATEGORY);
    }

    @Test
    @DisplayName("DB에 존재하지 않는 선호 음식 타입이면 예외가 발생한다.")
    void createCharacteristics_invalidLikeFoodCategory() {
        // given
        Long userId = 1L;
        List<FoodType> likeFoods = List.of(FoodType.KOREAN, FoodType.CHINESE);

        UserCharacteristicsCreateServiceRequest request = new UserCharacteristicsCreateServiceRequest(
            List.of(), likeFoods, List.of(), null
        );

        User user = UserFixture.create(userId);
        LikeFoodCategory food1 = new LikeFoodCategory(FoodType.KOREAN);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(likeFoodCategoryRepository.findByCategoryIn(likeFoods)).willReturn(List.of(food1));

        // when // then
        assertThatThrownBy(() -> userService.createCharacteristics(userId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", INVALID_CATEGORY);
    }

    @Test
    @DisplayName("DB에 존재하지 않는 선호 재료 타입이면 예외가 발생한다.")
    void createCharacteristics_invalidLikeIngredientCategory() {
        // given
        Long userId = 1L;
        List<IngredientType> likeIngredients = List.of(IngredientType.MEAT, IngredientType.SEAFOOD);

        UserCharacteristicsCreateServiceRequest request = new UserCharacteristicsCreateServiceRequest(
            List.of(), List.of(), likeIngredients, null
        );

        User user = UserFixture.create(userId);
        LikeIngredientCategory ingredient1 = new LikeIngredientCategory(IngredientType.MEAT);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(likeIngredientCategoryRepository.findByCategoryIn(likeIngredients)).willReturn(List.of(ingredient1));

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

    @Test
    @DisplayName("사용자 기본 정보를 성공적으로 조회한다.")
    void getUserBasic_success() {
        // given
        Long userId = 1L;
        User user = UserFixture.create(userId);
        user.updateBasic("맛집탐험가", Gender.FEMALE, AgeGroup.THIRTIES);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        UserBasicResponse response = userService.getUserBasic(userId);

        // then
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.nickname()).isEqualTo("맛집탐험가");
        assertThat(response.gender()).isEqualTo(Gender.FEMALE);
        assertThat(response.ageGroup()).isEqualTo(AgeGroup.THIRTIES);

        then(userRepository).should().findById(userId);
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 기본 정보를 조회할 수 없다.")
    void getUserBasic_userNotFound() {
        // given
        Long userId = 999L;

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> userService.getUserBasic(userId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", USER_NOT_FOUND);

        then(userRepository).should().findById(userId);
    }

    @Test
    @DisplayName("사용자 개인 특성을 성공적으로 수정한다 - 새로운 카테고리 추가")
    void updateUserCharacteristics_addNewCategories() {
        // given
        Long userId = 1L;
        User user = UserFixture.create(userId);

        List<AllergyType> newAllergies = List.of(AllergyType.SHRIMP, AllergyType.CRAB);
        List<FoodType> newLikeFoods = List.of(FoodType.KOREAN, FoodType.CHINESE);
        List<IngredientType> newLikeIngredients = List.of(IngredientType.MEAT, IngredientType.SEAFOOD);
        String otherCharacteristics = "매운 음식을 좋아합니다";

        UserCharacteristicsUpdateServiceRequest request = new UserCharacteristicsUpdateServiceRequest(
            newAllergies, newLikeFoods, newLikeIngredients, otherCharacteristics
        );

        AllergyCategory allergy1 = CategoryFixture.createAllergyCategory(1, AllergyType.SHRIMP);
        AllergyCategory allergy2 = CategoryFixture.createAllergyCategory(2, AllergyType.CRAB);
        LikeFoodCategory food1 = CategoryFixture.createLikeFoodCategory(1, FoodType.KOREAN);
        LikeFoodCategory food2 = CategoryFixture.createLikeFoodCategory(2, FoodType.CHINESE);
        LikeIngredientCategory ingredient1 = CategoryFixture.createLikeIngredientCategory(1, IngredientType.MEAT);
        LikeIngredientCategory ingredient2 = CategoryFixture.createLikeIngredientCategory(2, IngredientType.SEAFOOD);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userAllergyRepository.findByUserIdWithCategory(userId)).willReturn(List.of());
        given(userLikeFoodRepository.findByUserIdWithCategory(userId)).willReturn(List.of());
        given(userLikeIngredientRepository.findByUserIdWithCategory(userId)).willReturn(List.of());
        given(allergyCategoryRepository.findByCategoryIn(anyList())).willReturn(List.of(allergy1, allergy2));
        given(likeFoodCategoryRepository.findByCategoryIn(anyList())).willReturn(List.of(food1, food2));
        given(likeIngredientCategoryRepository.findByCategoryIn(anyList())).willReturn(List.of(ingredient1, ingredient2));
        given(snowflake.nextId()).willReturn(100L, 101L, 102L, 103L, 104L, 105L);

        // when
        userService.updateUserCharacteristics(userId, request);

        // then
        assertThat(user.getOtherCharacteristics()).isEqualTo(otherCharacteristics);

        then(userAllergyRepository).should().saveAll(userAllergyCaptor.capture());
        then(userLikeFoodRepository).should().saveAll(userLikeFoodCaptor.capture());
        then(userLikeIngredientRepository).should().saveAll(userLikeIngredientCaptor.capture());

        assertSavedAllergies(userAllergyCaptor.getValue(), AllergyType.SHRIMP, AllergyType.CRAB);
        assertSavedLikeFoods(userLikeFoodCaptor.getValue(), FoodType.KOREAN, FoodType.CHINESE);
        assertSavedLikeIngredients(userLikeIngredientCaptor.getValue(), IngredientType.MEAT, IngredientType.SEAFOOD);

        then(commonEventPublisher).should().publish(eq(EventType.USER_PERSONA), userPersonaPayloadCaptor.capture());
        UserPersonaPayload capturedPayload = userPersonaPayloadCaptor.getValue();
        assertThat(capturedPayload.user()).isEqualTo(user);
        assertThat(capturedPayload.allergies()).isEqualTo(newAllergies);
        assertThat(capturedPayload.likeFoods()).isEqualTo(newLikeFoods);
        assertThat(capturedPayload.likeIngredients()).isEqualTo(newLikeIngredients);
    }

    @Test
    @DisplayName("사용자 개인 특성을 성공적으로 수정한다 - 기존 카테고리 삭제")
    void updateUserCharacteristics_removeExistingCategories() {
        // given
        Long userId = 1L;
        User user = UserFixture.create(userId);

        List<AllergyType> newAllergies = List.of();
        List<FoodType> newLikeFoods = List.of(FoodType.KOREAN);
        List<IngredientType> newLikeIngredients = List.of(IngredientType.MEAT);

        UserCharacteristicsUpdateServiceRequest request = new UserCharacteristicsUpdateServiceRequest(
            newAllergies, newLikeFoods, newLikeIngredients, null
        );

        AllergyCategory existingAllergy = CategoryFixture.createAllergyCategory(1, AllergyType.SHRIMP);
        LikeFoodCategory existingFood1 = CategoryFixture.createLikeFoodCategory(1, FoodType.KOREAN);
        LikeFoodCategory existingFood2 = CategoryFixture.createLikeFoodCategory(2, FoodType.CHINESE);
        LikeIngredientCategory existingIngredient = CategoryFixture.createLikeIngredientCategory(1, IngredientType.MEAT);

        List<UserAllergy> existingUserAllergies = List.of(new UserAllergy(100L, user, existingAllergy));
        List<UserLikeFood> existingUserLikeFoods = List.of(
            new UserLikeFood(200L, user, existingFood1),
            new UserLikeFood(201L, user, existingFood2)
        );
        List<UserLikeIngredient> existingUserLikeIngredients = List.of(
            new UserLikeIngredient(300L, user, existingIngredient)
        );

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userAllergyRepository.findByUserIdWithCategory(userId)).willReturn(existingUserAllergies);
        given(userLikeFoodRepository.findByUserIdWithCategory(userId)).willReturn(existingUserLikeFoods);
        given(userLikeIngredientRepository.findByUserIdWithCategory(userId)).willReturn(existingUserLikeIngredients);
        given(allergyCategoryRepository.findByCategoryIn(anyList())).willReturn(List.of(existingAllergy));
        given(likeFoodCategoryRepository.findByCategoryIn(anyList())).willReturn(List.of(existingFood2));

        // when
        userService.updateUserCharacteristics(userId, request);

        // then
        then(userAllergyRepository).should().deleteAllByUserAndAllergyCategoryIn(eq(user), allergyCategoryCaptor.capture());
        then(userLikeFoodRepository).should().deleteAllByUserAndLikeFoodCategoryIn(eq(user), likeFoodCategoryCaptor.capture());
        then(userLikeIngredientRepository).should(never()).deleteAllByUserAndLikeIngredientCategoryIn(any(), anyList());

        assertDeletedAllergies(allergyCategoryCaptor.getValue(), AllergyType.SHRIMP);
        assertDeletedLikeFoods(likeFoodCategoryCaptor.getValue(), FoodType.CHINESE);

        then(commonEventPublisher).should().publish(eq(EventType.USER_PERSONA), userPersonaPayloadCaptor.capture());
        UserPersonaPayload capturedPayload = userPersonaPayloadCaptor.getValue();
        assertThat(capturedPayload.user()).isEqualTo(user);
        assertThat(capturedPayload.allergies()).isEqualTo(newAllergies);
        assertThat(capturedPayload.likeFoods()).isEqualTo(newLikeFoods);
        assertThat(capturedPayload.likeIngredients()).isEqualTo(newLikeIngredients);
    }

    @Test
    @DisplayName("사용자 개인 특성을 성공적으로 수정한다 - 일부 추가, 일부 삭제")
    void updateUserCharacteristics_addAndRemove() {
        // given
        Long userId = 1L;
        User user = UserFixture.create(userId);

        List<AllergyType> newAllergies = List.of(AllergyType.CRAB, AllergyType.EGG);
        List<FoodType> newLikeFoods = List.of(FoodType.KOREAN);
        List<IngredientType> newLikeIngredients = List.of(IngredientType.SEAFOOD);
        String otherCharacteristics = "업데이트된 특성";

        UserCharacteristicsUpdateServiceRequest request = new UserCharacteristicsUpdateServiceRequest(
            newAllergies, newLikeFoods, newLikeIngredients, otherCharacteristics
        );

        AllergyCategory existingAllergy = CategoryFixture.createAllergyCategory(1, AllergyType.SHRIMP);
        AllergyCategory newAllergy1 = CategoryFixture.createAllergyCategory(2, AllergyType.CRAB);
        AllergyCategory newAllergy2 = CategoryFixture.createAllergyCategory(3, AllergyType.EGG);
        LikeFoodCategory existingFood = CategoryFixture.createLikeFoodCategory(1, FoodType.KOREAN);
        LikeIngredientCategory existingIngredient = CategoryFixture.createLikeIngredientCategory(1, IngredientType.MEAT);
        LikeIngredientCategory newIngredient = CategoryFixture.createLikeIngredientCategory(2, IngredientType.SEAFOOD);

        List<UserAllergy> existingUserAllergies = List.of(new UserAllergy(100L, user, existingAllergy));
        List<UserLikeFood> existingUserLikeFoods = List.of(new UserLikeFood(200L, user, existingFood));
        List<UserLikeIngredient> existingUserLikeIngredients = List.of(new UserLikeIngredient(300L, user, existingIngredient));

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userAllergyRepository.findByUserIdWithCategory(userId)).willReturn(existingUserAllergies);
        given(userLikeFoodRepository.findByUserIdWithCategory(userId)).willReturn(existingUserLikeFoods);
        given(userLikeIngredientRepository.findByUserIdWithCategory(userId)).willReturn(existingUserLikeIngredients);
        given(allergyCategoryRepository.findByCategoryIn(anyList())).willReturn(List.of(existingAllergy), List.of(newAllergy1, newAllergy2));
        given(likeIngredientCategoryRepository.findByCategoryIn(anyList())).willReturn(List.of(existingIngredient), List.of(newIngredient));
        given(snowflake.nextId()).willReturn(101L, 102L, 301L);

        // when
        userService.updateUserCharacteristics(userId, request);

        // then
        assertThat(user.getOtherCharacteristics()).isEqualTo(otherCharacteristics);

        then(userAllergyRepository).should().deleteAllByUserAndAllergyCategoryIn(eq(user), allergyCategoryCaptor.capture());
        then(userAllergyRepository).should().saveAll(userAllergyCaptor.capture());
        then(userLikeFoodRepository).should(never()).deleteAllByUserAndLikeFoodCategoryIn(any(), anyList());
        then(userLikeFoodRepository).should(never()).saveAll(anyList());
        then(userLikeIngredientRepository).should().deleteAllByUserAndLikeIngredientCategoryIn(eq(user), likeIngredientCategoryCaptor.capture());
        then(userLikeIngredientRepository).should().saveAll(userLikeIngredientCaptor.capture());

        assertDeletedAllergies(allergyCategoryCaptor.getValue(), AllergyType.SHRIMP);
        assertSavedAllergies(userAllergyCaptor.getValue(), AllergyType.CRAB, AllergyType.EGG);
        assertDeletedLikeIngredients(likeIngredientCategoryCaptor.getValue(), IngredientType.MEAT);
        assertSavedLikeIngredients(userLikeIngredientCaptor.getValue(), IngredientType.SEAFOOD);

        then(commonEventPublisher).should().publish(eq(EventType.USER_PERSONA), userPersonaPayloadCaptor.capture());
        UserPersonaPayload capturedPayload = userPersonaPayloadCaptor.getValue();
        assertThat(capturedPayload.user()).isEqualTo(user);
        assertThat(capturedPayload.allergies()).isEqualTo(newAllergies);
        assertThat(capturedPayload.likeFoods()).isEqualTo(newLikeFoods);
        assertThat(capturedPayload.likeIngredients()).isEqualTo(newLikeIngredients);
    }

    @Test
    @DisplayName("알레르기를 null로 보내면 기존 알레르기가 모두 삭제된다.")
    void updateUserCharacteristics_nullAllergies() {
        // given
        Long userId = 1L;
        User user = UserFixture.create(userId);

        List<FoodType> newLikeFoods = List.of(FoodType.KOREAN);
        List<IngredientType> newLikeIngredients = List.of(IngredientType.MEAT);

        UserCharacteristicsUpdateServiceRequest request = new UserCharacteristicsUpdateServiceRequest(
            null, newLikeFoods, newLikeIngredients, null
        );

        AllergyCategory existingAllergy = CategoryFixture.createAllergyCategory(1, AllergyType.SHRIMP);
        LikeFoodCategory existingFood = CategoryFixture.createLikeFoodCategory(1, FoodType.KOREAN);
        LikeIngredientCategory existingIngredient = CategoryFixture.createLikeIngredientCategory(1, IngredientType.MEAT);

        List<UserAllergy> existingUserAllergies = List.of(new UserAllergy(100L, user, existingAllergy));
        List<UserLikeFood> existingUserLikeFoods = List.of(new UserLikeFood(200L, user, existingFood));
        List<UserLikeIngredient> existingUserLikeIngredients = List.of(new UserLikeIngredient(300L, user, existingIngredient));

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userAllergyRepository.findByUserIdWithCategory(userId)).willReturn(existingUserAllergies);
        given(userLikeFoodRepository.findByUserIdWithCategory(userId)).willReturn(existingUserLikeFoods);
        given(userLikeIngredientRepository.findByUserIdWithCategory(userId)).willReturn(existingUserLikeIngredients);
        given(allergyCategoryRepository.findByCategoryIn(anyList())).willReturn(List.of(existingAllergy));

        // when
        userService.updateUserCharacteristics(userId, request);

        // then
        then(userAllergyRepository).should().deleteAllByUserAndAllergyCategoryIn(eq(user), allergyCategoryCaptor.capture());
        then(userAllergyRepository).should(never()).saveAll(anyList());

        assertDeletedAllergies(allergyCategoryCaptor.getValue(), AllergyType.SHRIMP);

        then(commonEventPublisher).should().publish(eq(EventType.USER_PERSONA), userPersonaPayloadCaptor.capture());
        UserPersonaPayload capturedPayload = userPersonaPayloadCaptor.getValue();
        assertThat(capturedPayload.user()).isEqualTo(user);
        assertThat(capturedPayload.allergies()).isNull();
        assertThat(capturedPayload.likeFoods()).isEqualTo(newLikeFoods);
        assertThat(capturedPayload.likeIngredients()).isEqualTo(newLikeIngredients);
    }

    @Test
    @DisplayName("동일한 카테고리면 추가/삭제 없이 완료된다.")
    void updateUserCharacteristics_noChanges() {
        // given
        Long userId = 1L;
        User user = UserFixture.create(userId);

        List<AllergyType> allergies = List.of(AllergyType.SHRIMP);
        List<FoodType> likeFoods = List.of(FoodType.KOREAN);
        List<IngredientType> likeIngredients = List.of(IngredientType.MEAT);

        UserCharacteristicsUpdateServiceRequest request = new UserCharacteristicsUpdateServiceRequest(
            allergies, likeFoods, likeIngredients, "기존 특성"
        );

        AllergyCategory existingAllergy = CategoryFixture.createAllergyCategory(1, AllergyType.SHRIMP);
        LikeFoodCategory existingFood = CategoryFixture.createLikeFoodCategory(1, FoodType.KOREAN);
        LikeIngredientCategory existingIngredient = CategoryFixture.createLikeIngredientCategory(1, IngredientType.MEAT);

        List<UserAllergy> existingUserAllergies = List.of(new UserAllergy(100L, user, existingAllergy));
        List<UserLikeFood> existingUserLikeFoods = List.of(new UserLikeFood(200L, user, existingFood));
        List<UserLikeIngredient> existingUserLikeIngredients = List.of(new UserLikeIngredient(300L, user, existingIngredient));

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userAllergyRepository.findByUserIdWithCategory(userId)).willReturn(existingUserAllergies);
        given(userLikeFoodRepository.findByUserIdWithCategory(userId)).willReturn(existingUserLikeFoods);
        given(userLikeIngredientRepository.findByUserIdWithCategory(userId)).willReturn(existingUserLikeIngredients);

        // when
        userService.updateUserCharacteristics(userId, request);

        // then
        assertThat(user.getOtherCharacteristics()).isEqualTo("기존 특성");

        then(userAllergyRepository).should(never()).deleteAllByUserAndAllergyCategoryIn(any(), anyList());
        then(userAllergyRepository).should(never()).saveAll(anyList());
        then(userLikeFoodRepository).should(never()).deleteAllByUserAndLikeFoodCategoryIn(any(), anyList());
        then(userLikeFoodRepository).should(never()).saveAll(anyList());
        then(userLikeIngredientRepository).should(never()).deleteAllByUserAndLikeIngredientCategoryIn(any(), anyList());
        then(userLikeIngredientRepository).should(never()).saveAll(anyList());

        then(commonEventPublisher).should().publish(eq(EventType.USER_PERSONA), userPersonaPayloadCaptor.capture());
        UserPersonaPayload capturedPayload = userPersonaPayloadCaptor.getValue();
        assertThat(capturedPayload.user()).isEqualTo(user);
        assertThat(capturedPayload.allergies()).isEqualTo(allergies);
        assertThat(capturedPayload.likeFoods()).isEqualTo(likeFoods);
        assertThat(capturedPayload.likeIngredients()).isEqualTo(likeIngredients);
    }

    @Test
    @DisplayName("특성 수정 시 알레르기 타입이 중복되면 예외가 발생한다.")
    void updateUserCharacteristics_duplicateAllergies() {
        // given
        Long userId = 1L;
        List<AllergyType> duplicateAllergies = List.of(AllergyType.SHRIMP, AllergyType.SHRIMP);

        UserCharacteristicsUpdateServiceRequest request = new UserCharacteristicsUpdateServiceRequest(
            duplicateAllergies, List.of(FoodType.KOREAN), List.of(IngredientType.MEAT), null
        );

        // when // then
        assertThatThrownBy(() -> userService.updateUserCharacteristics(userId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DUPLICATE_ALLERGY_CATEGORY);

        then(userRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("특성 수정 시 선호 음식 타입이 중복되면 예외가 발생한다.")
    void updateUserCharacteristics_duplicateLikeFoods() {
        // given
        Long userId = 1L;
        List<FoodType> duplicateFoods = List.of(FoodType.KOREAN, FoodType.KOREAN);

        UserCharacteristicsUpdateServiceRequest request = new UserCharacteristicsUpdateServiceRequest(
            List.of(), duplicateFoods, List.of(IngredientType.MEAT), null
        );

        // when // then
        assertThatThrownBy(() -> userService.updateUserCharacteristics(userId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DUPLICATE_LIKE_FOOD_CATEGORY);

        then(userRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("특성 수정 시 선호 재료 타입이 중복되면 예외가 발생한다.")
    void updateUserCharacteristics_duplicateLikeIngredients() {
        // given
        Long userId = 1L;
        List<IngredientType> duplicateIngredients = List.of(IngredientType.MEAT, IngredientType.MEAT);

        UserCharacteristicsUpdateServiceRequest request = new UserCharacteristicsUpdateServiceRequest(
            List.of(), List.of(FoodType.KOREAN), duplicateIngredients, null
        );

        // when // then
        assertThatThrownBy(() -> userService.updateUserCharacteristics(userId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", DUPLICATE_LIKE_INGREDIENT_CATEGORY);

        then(userRepository).should(never()).findById(any());
    }

    @Test
    @DisplayName("특성 수정 시 존재하지 않는 사용자면 예외가 발생한다.")
    void updateUserCharacteristics_userNotFound() {
        // given
        Long userId = 999L;

        UserCharacteristicsUpdateServiceRequest request = new UserCharacteristicsUpdateServiceRequest(
            List.of(), List.of(FoodType.KOREAN), List.of(IngredientType.MEAT), null
        );

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> userService.updateUserCharacteristics(userId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", USER_NOT_FOUND);

        then(userRepository).should().findById(userId);
    }

    @Test
    @DisplayName("특성 수정 시 DB에 존재하지 않는 알레르기 카테고리면 예외가 발생한다.")
    void updateUserCharacteristics_invalidAllergyCategory() {
        // given
        Long userId = 1L;
        User user = UserFixture.create(userId);

        List<AllergyType> newAllergies = List.of(AllergyType.SHRIMP, AllergyType.CRAB);
        List<FoodType> newLikeFoods = List.of(FoodType.KOREAN);
        List<IngredientType> newLikeIngredients = List.of(IngredientType.MEAT);

        UserCharacteristicsUpdateServiceRequest request = new UserCharacteristicsUpdateServiceRequest(
            newAllergies, newLikeFoods, newLikeIngredients, null
        );

        AllergyCategory allergy1 = CategoryFixture.createAllergyCategory(1, AllergyType.SHRIMP);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userAllergyRepository.findByUserIdWithCategory(userId)).willReturn(List.of());
        given(allergyCategoryRepository.findByCategoryIn(anyList())).willReturn(List.of(allergy1));

        // when // then
        assertThatThrownBy(() -> userService.updateUserCharacteristics(userId, request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", INVALID_CATEGORY);
    }

    // ===== Withdraw Tests =====

    @Test
    @DisplayName("회원 탈퇴를 성공적으로 처리한다.")
    void withdraw_success() {
        // given
        Long userId = 1L;
        User user = UserFixture.create(userId);
        RefreshToken refreshToken = new RefreshToken(user.getEmail(), "some-token");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        willDoNothing().given(kakaoUtil).kakaoUnlink(user.getProviderId());
        given(refreshTokenRepository.findById(user.getEmail())).willReturn(Optional.of(refreshToken));

        // when
        userService.withdraw(userId);

        // then
        assertThat(user.isWithdraw()).isTrue();
        assertThat(user.getWithdrawAt()).isNotNull();

        then(kakaoUtil).should().kakaoUnlink(user.getProviderId());
        then(refreshTokenRepository).should().findById(user.getEmail());
        then(refreshTokenRepository).should().delete(refreshToken);
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 탈퇴할 수 없다.")
    void withdraw_userNotFound() {
        // given
        Long userId = 999L;

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when // then
        assertThatThrownBy(() -> userService.withdraw(userId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", USER_NOT_FOUND);

        then(kakaoUtil).should(never()).kakaoUnlink(any());
    }

    @Test
    @DisplayName("이미 탈퇴한 사용자는 다시 탈퇴할 수 없다.")
    void withdraw_alreadyWithdrawn() {
        // given
        Long userId = 1L;
        User user = UserFixture.create(userId);
        user.withdraw();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when // then
        assertThatThrownBy(() -> userService.withdraw(userId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", ALREADY_WITHDRAWN);

        then(kakaoUtil).should(never()).kakaoUnlink(any());
    }

    @Test
    @DisplayName("카카오 연동 해제 실패 시 탈퇴가 롤백된다.")
    void withdraw_kakaoUnlinkFailed() {
        // given
        Long userId = 1L;
        User user = UserFixture.create(userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        willThrow(new CustomException(KAKAO_UNLINK_FAILED)).given(kakaoUtil).kakaoUnlink(user.getProviderId());

        // when // then
        assertThatThrownBy(() -> userService.withdraw(userId))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", KAKAO_UNLINK_FAILED);

        assertThat(user.isWithdraw()).isFalse();

        then(refreshTokenRepository).should(never()).findById(any());
    }
}
