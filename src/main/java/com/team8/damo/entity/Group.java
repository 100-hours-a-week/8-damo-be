package com.team8.damo.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "groups")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Group extends BaseTimeEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "introduction", length = 30)
    private String introduction;

    @Column(name = "total_members", nullable = false)
    private int totalMembers = 1;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    public Group(Long id, String name, String introduction, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.introduction = introduction;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void incrementTotalMembers() {
        this.totalMembers++;
    }
}
