package com.team8.damo.repository;

import com.team8.damo.entity.LikeFoodCategory;
import com.team8.damo.entity.User;
import com.team8.damo.entity.UserLikeFood;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserLikeFoodRepository extends JpaRepository<UserLikeFood, Long> {

    List<UserLikeFood> findByUser(User user);

    void deleteByUser(User user);

    @Query("SELECT ulf FROM UserLikeFood ulf JOIN FETCH ulf.likeFoodCategory WHERE ulf.user.id = :userId")
    List<UserLikeFood> findByUserIdWithCategory(@Param("userId") Long userId);

    void deleteAllByUserAndLikeFoodCategoryIn(User user, List<LikeFoodCategory> categories);
}
