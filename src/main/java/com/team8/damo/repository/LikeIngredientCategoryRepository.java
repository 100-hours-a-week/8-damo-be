package com.team8.damo.repository;

import com.team8.damo.entity.LikeIngredientCategory;
import com.team8.damo.entity.enumeration.IngredientType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LikeIngredientCategoryRepository extends JpaRepository<LikeIngredientCategory, Integer> {

    Optional<LikeIngredientCategory> findByCategory(IngredientType category);

    List<LikeIngredientCategory> findByCategoryIn(List<IngredientType> categories);
}
