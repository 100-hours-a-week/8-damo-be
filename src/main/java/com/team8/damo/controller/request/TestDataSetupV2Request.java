package com.team8.damo.controller.request;

import com.team8.damo.entity.enumeration.SetupPhase;

import java.util.List;

public record TestDataSetupV2Request(
    boolean dryRun,
    boolean repair,
    List<SetupPhase> phases,
    Integer maxWorkers,
    Integer messageBatchSize
) {
}
