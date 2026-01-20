package com.team8.damo.repository;

import com.team8.damo.entity.LikeIngredientCategory;
import com.team8.damo.entity.enumeration.IngredientType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeIngredientCategoryRepository extends JpaRepository<LikeIngredientCategory, Integer> {

    Optional<LikeIngredientCategory> findByCategory(IngredientType category);
}
