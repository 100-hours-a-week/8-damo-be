package com.team8.damo.controller.request;

public record TestDataCleanupRequest(
    String prefix,
    boolean dryRun
) {
}
