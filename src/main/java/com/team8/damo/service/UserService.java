package com.team8.damo.service;

import com.team8.damo.client.AiService;
import com.team8.damo.entity.*;
import com.team8.damo.entity.enumeration.AllergyType;
import com.team8.damo.entity.enumeration.FoodType;
import com.team8.damo.entity.enumeration.IngredientType;
import com.team8.damo.entity.enumeration.OnboardingStep;
import com.team8.damo.exception.CustomException;
import com.team8.damo.exception.errorcode.ErrorCode;
import com.team8.damo.repository.*;
import com.team8.damo.service.request.UserBasicUpdateServiceRequest;
import com.team8.damo.service.request.UserCharacteristicsCreateServiceRequest;
import com.team8.damo.service.response.UserBasicResponse;
import com.team8.damo.service.response.UserProfileResponse;
import com.team8.damo.util.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

import static com.team8.damo.exception.errorcode.ErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final AllergyCategoryRepository allergyCategoryRepository;
    private final LikeFoodCategoryRepository likeFoodCategoryRepository;
    private final LikeIngredientCategoryRepository likeIngredientCategoryRepository;
    private final UserAllergyRepository userAllergyRepository;
    private final UserLikeFoodRepository userLikeFoodRepository;
    private final UserLikeIngredientRepository userLikeIngredientRepository;
    private final Snowflake snowflake;
    private final AiService aiService;

    @Transactional
    public void updateUserBasic(Long userId, UserBasicUpdateServiceRequest request) {
        if (userRepository.existsByNickname(request.nickname())) {
            throw new CustomException(DUPLICATE_NICKNAME);
        }

        User user = findUserBy(userId);

        user.updateBasic(request.nickname(), request.gender(), request.ageGroup());
        user.changeImagePath(request.imagePath());
    }

    @Transactional
    public void createCharacteristics(Long userId, UserCharacteristicsCreateServiceRequest request) {
        validateNoDuplicates(request.allergies(), DUPLICATE_ALLERGY_CATEGORY);
        validateNoDuplicates(request.likeFoods(), DUPLICATE_LIKE_FOOD_CATEGORY);
        validateNoDuplicates(request.likeIngredients(), DUPLICATE_LIKE_INGREDIENT_CATEGORY);

        User user = findUserBy(userId);

        saveUserAllergies(user, request.allergies());
        saveUserLikeFoods(user, request.likeFoods());
        saveUserLikeIngredients(user, request.likeIngredients());

        user.updateOtherCharacteristics(request.otherCharacteristics());
        user.updateOnboardingStep(OnboardingStep.DONE);

        aiService.userPersonaUpdate(user, request.allergies(), request.likeFoods(), request.likeIngredients());
    }

    @Transactional
    public void changeImagePath(Long userId, String imagePath) {
        User user = findUserBy(userId);
        user.changeImagePath(imagePath);
        // 기존 이미지 삭제
    }

    private <T> void validateNoDuplicates(List<T> items, ErrorCode errorCode) {
        if (items == null || items.isEmpty()) {
            return;
        }
        if (new HashSet<>(items).size() != items.size()) {
            throw new CustomException(errorCode);
        }
    }

    private void saveUserAllergies(User user, List<AllergyType> allergies) {
        if (allergies == null || allergies.isEmpty()) {
            return;
        }
        List<AllergyCategory> categories = allergyCategoryRepository.findByCategoryIn(allergies);
        if (categories.size() != allergies.size()) {
            throw new CustomException(INVALID_CATEGORY);
        }
        List<UserAllergy> userAllergies = categories.stream()
            .map(category -> new UserAllergy(snowflake.nextId(), user, category))
            .toList();
        userAllergyRepository.saveAll(userAllergies);
    }

    private void saveUserLikeFoods(User user, List<FoodType> likeFoods) {
        if (likeFoods == null || likeFoods.isEmpty()) {
            return;
        }
        List<LikeFoodCategory> categories = likeFoodCategoryRepository.findByCategoryIn(likeFoods);
        if (categories.size() != likeFoods.size()) {
            throw new CustomException(INVALID_CATEGORY);
        }
        List<UserLikeFood> userLikeFoods = categories.stream()
            .map(category -> new UserLikeFood(snowflake.nextId(), user, category))
            .toList();
        userLikeFoodRepository.saveAll(userLikeFoods);
    }

    private void saveUserLikeIngredients(User user, List<IngredientType> likeIngredients) {
        if (likeIngredients == null || likeIngredients.isEmpty()) {
            return;
        }
        List<LikeIngredientCategory> categories = likeIngredientCategoryRepository.findByCategoryIn(likeIngredients);
        if (categories.size() != likeIngredients.size()) {
            throw new CustomException(INVALID_CATEGORY);
        }
        List<UserLikeIngredient> userLikeIngredients = categories.stream()
            .map(category -> new UserLikeIngredient(snowflake.nextId(), user, category))
            .toList();
        userLikeIngredientRepository.saveAll(userLikeIngredients);
    }

    public UserProfileResponse getUserProfile(Long userId) {
        User user = findUserBy(userId);

        List<UserAllergy> userAllergies = userAllergyRepository.findByUserIdWithCategory(userId);
        List<UserLikeFood> userLikeFoods = userLikeFoodRepository.findByUserIdWithCategory(userId);
        List<UserLikeIngredient> userLikeIngredients = userLikeIngredientRepository.findByUserIdWithCategory(userId);

        List<AllergyCategory> allergies = userAllergies.stream()
            .map(UserAllergy::getAllergyCategory)
            .toList();
        List<LikeFoodCategory> likeFoods = userLikeFoods.stream()
            .map(UserLikeFood::getLikeFoodCategory)
            .toList();
        List<LikeIngredientCategory> likeIngredients = userLikeIngredients.stream()
            .map(UserLikeIngredient::getLikeIngredientCategory)
            .toList();

        return UserProfileResponse.of(user, allergies, likeFoods, likeIngredients);
    }

    private User findUserBy(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(USER_NOT_FOUND));
    }

    public UserBasicResponse getUserBasic(Long userId) {
        User user = findUserBy(userId);
        return UserBasicResponse.from(user);
    }
}
