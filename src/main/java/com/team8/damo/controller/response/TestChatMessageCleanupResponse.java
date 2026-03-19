package com.team8.damo.controller.response;

public record TestChatMessageCleanupResponse(
    String prefix,
    boolean dryRun,
    long chatMessageCount,
    long readStatusCount,
    long deletedChatMessageCount,
    long deletedReadStatusCount
) {
}
