package com.team8.damo.repository;

import com.team8.damo.entity.UserGroup;
import com.team8.damo.entity.enumeration.GroupRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {

    @EntityGraph(attributePaths = {"group"})
    @Query("select ug from UserGroup ug where ug.user.id = :userId")
    List<UserGroup> findAllByUserIdWithGroup(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"group"})
    @Query("select ug from UserGroup ug where ug.user.id = :userId order by ug.group.id desc")
    List<UserGroup> findAllByUserIdWithGroupCursor(@Param("userId") Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"group"})
    @Query("select ug from UserGroup ug where ug.user.id = :userId and ug.group.id < :lastGroupId order by ug.group.id desc")
    List<UserGroup> findAllByUserIdWithGroupCursorAfter(@Param("userId") Long userId, @Param("lastGroupId") Long lastGroupId, Pageable pageable);

    @EntityGraph(attributePaths = {"group"})
    Optional<UserGroup> findByUserIdAndGroupId(Long userId, Long groupId);

    boolean existsByUserIdAndGroupId(Long userId, Long groupId);

    boolean existsByUserIdAndGroupIdAndRole(Long userId, Long groupId, GroupRole role);

    @EntityGraph(attributePaths = {"user"})
    @Query("select ug from UserGroup ug where ug.group.id = :groupId")
    List<UserGroup> findAllByGroupIdWithUser(@Param("groupId") Long groupId);
}
