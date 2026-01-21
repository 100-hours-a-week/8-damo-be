package com.team8.damo.service;

import com.team8.damo.entity.*;
import com.team8.damo.entity.enumeration.OnboardingStep;
import com.team8.damo.exception.CustomException;
import com.team8.damo.exception.errorcode.ErrorCode;
import com.team8.damo.repository.*;
import com.team8.damo.service.request.UserBasicUpdateServiceRequest;
import com.team8.damo.service.request.UserCharacteristicsCreateServiceRequest;
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

    @Transactional
    public void updateUserBasic(Long userId, UserBasicUpdateServiceRequest request) {
        if (userRepository.existsByNickname(request.nickname())) {
            throw new CustomException(DUPLICATE_NICKNAME);
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        user.updateBasic(request.nickname(), request.gender(), request.ageGroup());
    }

    @Transactional
    public void createCharacteristics(Long userId, UserCharacteristicsCreateServiceRequest request) {
        validateNoDuplicates(request.allergyIds(), DUPLICATE_ALLERGY_CATEGORY);
        validateNoDuplicates(request.likeFoodIds(), DUPLICATE_LIKE_FOOD_CATEGORY);
        validateNoDuplicates(request.likeIngredientIds(), DUPLICATE_LIKE_INGREDIENT_CATEGORY);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        saveUserAllergies(user, request.allergyIds());
        saveUserLikeFoods(user, request.likeFoodIds());
        saveUserLikeIngredients(user, request.likeIngredientIds());

        user.updateOtherCharacteristics(request.otherCharacteristics());
        user.updateOnboardingStep(OnboardingStep.DONE);
    }

    private void validateNoDuplicates(List<Integer> ids, ErrorCode errorCode) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new CustomException(errorCode);
        }
    }

    private void saveUserAllergies(User user, List<Integer> allergyIds) {
        if (allergyIds == null || allergyIds.isEmpty()) {
            return;
        }
        List<AllergyCategory> categories = allergyCategoryRepository.findAllById(allergyIds);
        if (categories.size() != allergyIds.size()) {
            throw new CustomException(INVALID_CATEGORY);
        }
        List<UserAllergy> userAllergies = categories.stream()
            .map(category -> new UserAllergy(snowflake.nextId(), user, category))
            .toList();
        userAllergyRepository.saveAll(userAllergies);
    }

    private void saveUserLikeFoods(User user, List<Integer> likeFoodIds) {
        if (likeFoodIds == null || likeFoodIds.isEmpty()) {
            return;
        }
        List<LikeFoodCategory> categories = likeFoodCategoryRepository.findAllById(likeFoodIds);
        if (categories.size() != likeFoodIds.size()) {
            throw new CustomException(INVALID_CATEGORY);
        }
        List<UserLikeFood> userLikeFoods = categories.stream()
            .map(category -> new UserLikeFood(snowflake.nextId(), user, category))
            .toList();
        userLikeFoodRepository.saveAll(userLikeFoods);
    }

    private void saveUserLikeIngredients(User user, List<Integer> likeIngredientIds) {
        if (likeIngredientIds == null || likeIngredientIds.isEmpty()) {
            return;
        }
        List<LikeIngredientCategory> categories = likeIngredientCategoryRepository.findAllById(likeIngredientIds);
        if (categories.size() != likeIngredientIds.size()) {
            throw new CustomException(INVALID_CATEGORY);
        }
        List<UserLikeIngredient> userLikeIngredients = categories.stream()
            .map(category -> new UserLikeIngredient(snowflake.nextId(), user, category))
            .toList();
        userLikeIngredientRepository.saveAll(userLikeIngredients);
    }
}
