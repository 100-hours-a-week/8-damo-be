package com.team8.damo.repository;

import com.team8.damo.entity.User;
import com.team8.damo.entity.UserLikeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserLikeIngredientRepository extends JpaRepository<UserLikeIngredient, Long> {

    List<UserLikeIngredient> findByUser(User user);

    void deleteByUser(User user);

    @Query("SELECT uli FROM UserLikeIngredient uli JOIN FETCH uli.likeIngredientCategory WHERE uli.user.id = :userId")
    List<UserLikeIngredient> findByUserIdWithCategory(@Param("userId") Long userId);
}
