package com.team8.damo.entity;

import com.team8.damo.entity.enumeration.SatisfactionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "satisfaction_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SatisfactionCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(value = EnumType.STRING)
    private SatisfactionType category;

    public SatisfactionCategory(SatisfactionType category) {
        this.category = category;
    }
}
