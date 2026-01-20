package com.team8.damo.repository;

import com.team8.damo.entity.AllergyCategory;
import com.team8.damo.entity.enumeration.AllergyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AllergyCategoryRepository extends JpaRepository<AllergyCategory, Integer> {

    Optional<AllergyCategory> findByCategory(AllergyType category);
}
