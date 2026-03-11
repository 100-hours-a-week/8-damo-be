package com.team8.damo.controller.response;

public record TestDataCleanupV2Response(
    boolean dryRun,
    long deletedLightningCount,
    long deletedParticipantCount,
    long deletedChatMessageCount,
    long deletedUserCount
) {
}
