package com.team8.damo.controller.request;

public record TestDataCleanupV2Request(
    boolean dryRun,
    boolean deleteUsers,
    boolean deleteLightnings,
    boolean deleteParticipants,
    boolean deleteChatMessages,
    Integer maxWorkers
) {
}
