package com.team8.damo.controller.internal;

import com.team8.damo.controller.request.TestDataCleanupV2Request;
import com.team8.damo.controller.response.TestDataCleanupV2Response;
import com.team8.damo.service.TestDataCleanupV2Service;
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
public class TestDataCleanupV2Controller {

    private final TestDataCleanupV2Service testDataCleanupV2Service;

    @PostMapping("/cleanup/v2")
    public TestDataCleanupV2Response cleanup(
        @RequestHeader("X-Test-Setup-Token") String setupToken,
        @RequestBody TestDataCleanupV2Request request
    ) {
        return testDataCleanupV2Service.cleanup(setupToken, request);
    }
}
