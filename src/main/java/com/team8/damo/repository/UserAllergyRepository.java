package com.team8.damo.repository;

import com.team8.damo.entity.AllergyCategory;
import com.team8.damo.entity.User;
import com.team8.damo.entity.UserAllergy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserAllergyRepository extends JpaRepository<UserAllergy, Long> {

    List<UserAllergy> findByUser(User user);

    void deleteByUser(User user);

    @Query("SELECT ua FROM UserAllergy ua JOIN FETCH ua.allergyCategory WHERE ua.user.id = :userId")
    List<UserAllergy> findByUserIdWithCategory(@Param("userId") Long userId);

    void deleteAllByUserAndAllergyCategoryIn(User user, List<AllergyCategory> categories);
}
