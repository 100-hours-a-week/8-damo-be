package com.team8.damo.fixture;

import com.team8.damo.entity.AllergyCategory;
import com.team8.damo.entity.LikeFoodCategory;
import com.team8.damo.entity.LikeIngredientCategory;
import com.team8.damo.entity.enumeration.AllergyType;
import com.team8.damo.entity.enumeration.FoodType;
import com.team8.damo.entity.enumeration.IngredientType;
import org.springframework.test.util.ReflectionTestUtils;

public class CategoryFixture {

    public static AllergyCategory createAllergyCategory(Integer id, AllergyType type) {
        AllergyCategory category = new AllergyCategory(type);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    public static LikeFoodCategory createLikeFoodCategory(Integer id, FoodType type) {
        LikeFoodCategory category = new LikeFoodCategory(type);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    public static LikeIngredientCategory createLikeIngredientCategory(Integer id, IngredientType type) {
        LikeIngredientCategory category = new LikeIngredientCategory(type);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }
}
