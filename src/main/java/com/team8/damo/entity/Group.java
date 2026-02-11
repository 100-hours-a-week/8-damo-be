package com.team8.damo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "`groups`")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Group extends BaseTimeEntity implements Persistable<Long> {

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

    @Column(name = "image_path", length = 200)
    private String imagePath;

    private static final int MAX_CAPACITY = 8;

    @Builder
    public Group(Long id, String name, String introduction, double latitude, double longitude, String imagePath, int totalMembers) {
        this.id = id;
        this.name = name;
        this.introduction = introduction;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imagePath = imagePath;
        this.totalMembers = totalMembers == 0 ? 1 : totalMembers;
    }

    public void incrementTotalMembers() {
        this.totalMembers++;
    }

    public void changeImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public boolean isOverCapacity() {
        return totalMembers >= MAX_CAPACITY;
    }

    @Override
    public boolean isNew() {
        return getCreatedAt() == null;
    }
}
