package com.team8.damo.controller.response;

public record TestDataCleanupResponse(
    String prefix,
    boolean dryRun,
    long lightningCount,
    long participantCount,
    long chatMessageCount,
    long deletedLightningCount,
    long deletedParticipantCount,
    long deletedChatMessageCount
) {
}
