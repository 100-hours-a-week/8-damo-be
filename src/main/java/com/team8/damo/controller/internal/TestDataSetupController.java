package com.team8.damo.controller.internal;

import com.team8.damo.controller.request.TestDataSetupRequest;
import com.team8.damo.controller.response.TestDataSetupResponse;
import com.team8.damo.service.TestDataSetupService;
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
public class TestDataSetupController {

    private final TestDataSetupService testDataSetupService;

    @PostMapping("/setup")
    public TestDataSetupResponse setup(
        @RequestHeader("X-Test-Setup-Token") String setupToken,
        @RequestBody TestDataSetupRequest request
    ) {
        return testDataSetupService.setup(setupToken, request);
    }
}
