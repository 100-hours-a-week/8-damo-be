package com.team8.damo.client.request;

import java.time.LocalDateTime;

public record DiningData(
    Long diningId,
    Long groupsId,
    LocalDateTime diningDate,
    Integer budget
) {
}
