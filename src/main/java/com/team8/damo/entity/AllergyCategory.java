package com.team8.damo.entity;

import com.team8.damo.entity.enumeration.AllergyType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.EnumType.STRING;

@Entity
@Table(name = "allergy_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AllergyCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(STRING)
    @Column(name = "category", nullable = false, length = 20,  unique = true)
    private AllergyType category;

    public AllergyCategory(AllergyType category) {
        this.category = category;
    }
}
