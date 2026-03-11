package com.team8.damo.controller.response;

import java.util.List;

public record TestDataSetupV2Response(
    boolean dryRun,
    boolean repair,
    long targetUserCount,
    long targetOpenLightningCount,
    long targetClosedLightningCount,
    long targetParticipantCount,
    long targetChatMessageCount,
    long createdUserCount,
    long updatedUserCount,
    long createdOpenLightningCount,
    long createdClosedLightningCount,
    long createdParticipantCount,
    long createdChatMessageCount,
    long finalUserCount,
    long finalOpenLightningCount,
    long finalClosedLightningCount,
    long finalParticipantCount,
    long finalChatMessageCount,
    List<String> completedPhases
) {
}
