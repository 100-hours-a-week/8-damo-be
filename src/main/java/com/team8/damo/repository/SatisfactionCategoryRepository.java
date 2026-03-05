package com.team8.damo.repository;

import com.team8.damo.entity.SatisfactionCategory;
import com.team8.damo.entity.enumeration.SatisfactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SatisfactionCategoryRepository extends JpaRepository<SatisfactionCategory, Long> {

    Optional<SatisfactionCategory> findByCategory(SatisfactionType category);

    List<SatisfactionCategory> findByCategoryIn(List<SatisfactionType> categories);
}
