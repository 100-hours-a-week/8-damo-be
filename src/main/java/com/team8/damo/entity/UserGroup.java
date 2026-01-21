package com.team8.damo.entity;

import com.team8.damo.entity.enumeration.GroupRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "users_groups",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UK_USERS_GROUPS",
            columnNames = {"users_id", "groups_id"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserGroup {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "groups_id", nullable = false)
    private Group group;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private GroupRole role;

    public UserGroup(Long id, User user, Group group, GroupRole role) {
        this.id = id;
        this.user = user;
        this.group = group;
        this.role = role;
    }
}
