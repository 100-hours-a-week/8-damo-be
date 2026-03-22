package com.team8.damo.controller.internal;

import com.team8.damo.controller.request.TestDataCleanupRequest;
import com.team8.damo.controller.response.TestDataCleanupResponse;
import com.team8.damo.service.TestDataCleanupService;
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
@RequestMapping("/api/internal/test-data/lightnings")
public class TestDataCleanupController {

    private final TestDataCleanupService testDataCleanupService;

    @PostMapping("/cleanup")
    public TestDataCleanupResponse cleanup(
        @RequestHeader("X-Test-Cleanup-Token") String cleanupToken,
        @RequestBody TestDataCleanupRequest request
    ) {
        System.out.println("cleanup token: " + cleanupToken);
        return testDataCleanupService.cleanup(cleanupToken, request);
    }
}
