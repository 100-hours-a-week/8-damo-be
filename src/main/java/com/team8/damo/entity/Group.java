package com.team8.damo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "`groups`")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Builder
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
