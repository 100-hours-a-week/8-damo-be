package com.team8.damo.entity.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IngredientType {
    MEAT("육류"),
    SEAFOOD("해산물"),
    VEGETABLE("채소"),
    DAIRY("유제품"),
    GRAIN("곡물"),
    POULTRY("가금류");

    private final String description;
}
