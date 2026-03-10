package com.team8.damo.controller.response;

public record TestDataSetupResponse(
    boolean dryRun,
    boolean repair,
    long targetUserCount,
    long createdUserCount,
    long finalUserCount,
    long targetLightningCount,
    long createdLightningCount,
    long finalLightningCount,
    long targetParticipantCount,
    long createdParticipantCount,
    long finalParticipantCount,
    long targetChatMessageCount,
    long createdChatMessageCount,
    long finalChatMessageCount
) {
}
