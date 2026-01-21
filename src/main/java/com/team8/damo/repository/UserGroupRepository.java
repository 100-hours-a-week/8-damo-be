package com.team8.damo.repository;

import com.team8.damo.entity.UserGroup;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {

    @EntityGraph(attributePaths = {"group"})
    @Query("select ug from UserGroup ug where ug.user.id = :userId")
    List<UserGroup> findAllByUserIdWithGroup(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"group"})
    Optional<UserGroup> findByUserIdAndGroupId(Long userId, Long groupId);
}
