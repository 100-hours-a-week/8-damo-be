package com.team8.damo.repository;

import com.team8.damo.entity.LikeFoodCategory;
import com.team8.damo.entity.enumeration.FoodType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LikeFoodCategoryRepository extends JpaRepository<LikeFoodCategory, Integer> {

    Optional<LikeFoodCategory> findByCategory(FoodType category);

    List<LikeFoodCategory> findByCategoryIn(List<FoodType> categories);
}
