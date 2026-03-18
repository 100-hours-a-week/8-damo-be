package com.team8.damo.controller.request;

public record TestChatMessageCleanupRequest(
    String prefix,
    boolean dryRun
) {
}
