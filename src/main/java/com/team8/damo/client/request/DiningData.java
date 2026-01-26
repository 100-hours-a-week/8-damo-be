package com.team8.damo.client.request;

import java.time.LocalDate;

public record DiningData(
        Long diningId,
        Long groupsId,
        LocalDate diningDate,
        Long budget
) {
}
