package com.team8.damo.service.request;

import com.team8.damo.entity.enumeration.SatisfactionType;

import java.util.List;

public record ReviewCreateServiceRequest(
    Integer starRating,
    List<SatisfactionType> satisfactions,
    String content
) {
}
