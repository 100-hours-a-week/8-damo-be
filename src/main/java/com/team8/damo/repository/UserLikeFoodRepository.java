package com.team8.damo.repository;

import com.team8.damo.entity.User;
import com.team8.damo.entity.UserLikeFood;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserLikeFoodRepository extends JpaRepository<UserLikeFood, Long> {

    List<UserLikeFood> findByUser(User user);

    void deleteByUser(User user);
}
