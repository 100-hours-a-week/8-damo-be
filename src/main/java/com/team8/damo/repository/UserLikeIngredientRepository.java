package com.team8.damo.repository;

import com.team8.damo.entity.User;
import com.team8.damo.entity.UserLikeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserLikeIngredientRepository extends JpaRepository<UserLikeIngredient, Long> {

    List<UserLikeIngredient> findByUser(User user);

    void deleteByUser(User user);
}
