package com.team8.damo.service.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CategoryResponse {
    private final Integer id;
    private final String category;

    public static CategoryResponse from(Integer id, String category) {
        return new CategoryResponse(id, category);
    }
}
