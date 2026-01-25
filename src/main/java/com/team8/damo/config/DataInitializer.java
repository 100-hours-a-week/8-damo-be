package com.team8.damo.config;

import com.team8.damo.entity.AllergyCategory;
import com.team8.damo.entity.LikeFoodCategory;
import com.team8.damo.entity.LikeIngredientCategory;
import com.team8.damo.entity.enumeration.AllergyType;
import com.team8.damo.entity.enumeration.FoodType;
import com.team8.damo.entity.enumeration.IngredientType;
import com.team8.damo.repository.AllergyCategoryRepository;
import com.team8.damo.repository.LikeFoodCategoryRepository;
import com.team8.damo.repository.LikeIngredientCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final AllergyCategoryRepository allergyCategoryRepository;
    private final LikeFoodCategoryRepository likeFoodCategoryRepository;
    private final LikeIngredientCategoryRepository likeIngredientCategoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initAllergyCategoriesIfEmpty();
        initLikeFoodCategoriesIfEmpty();
        initLikeIngredientCategoriesIfEmpty();
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
}
