package com.team8.damo.service.response;

import java.util.List;

public record DiningListResponse(
    List<DiningResponse> dinings
) {
    public static DiningListResponse of(List<DiningResponse> dinings) {
        return new DiningListResponse(dinings);
    }
}
