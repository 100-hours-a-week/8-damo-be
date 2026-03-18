package com.team8.damo.controller.internal;

import com.team8.damo.controller.request.TestChatMessageCleanupRequest;
import com.team8.damo.controller.response.TestChatMessageCleanupResponse;
import com.team8.damo.service.TestChatMessageCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile({"local", "dev"})
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/test-data/chat-messages")
public class TestChatMessageCleanupController {

    private final TestChatMessageCleanupService testChatMessageCleanupService;

    @PostMapping("/cleanup")
    public TestChatMessageCleanupResponse cleanup(
        @RequestHeader("X-Test-Cleanup-Token") String cleanupToken,
        @RequestBody TestChatMessageCleanupRequest request
    ) {
        System.out.println("cleanup request: " + request.prefix());
        return testChatMessageCleanupService.cleanup(cleanupToken, request);
    }
}
