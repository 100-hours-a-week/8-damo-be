package com.team8.damo.repository;

import com.team8.damo.entity.User;
import com.team8.damo.entity.UserAllergy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAllergyRepository extends JpaRepository<UserAllergy, Long> {

    List<UserAllergy> findByUser(User user);

    void deleteByUser(User user);
}
