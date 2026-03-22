package com.team8.damo.controller.request;

public record TestDataSetupRequest(
    boolean dryRun,
    boolean repair
) {
}
