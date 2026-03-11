package com.team8.damo.controller.internal;

import com.team8.damo.controller.request.TestDataSetupV2Request;
import com.team8.damo.controller.response.TestDataSetupV2Response;
import com.team8.damo.service.TestDataSetupV2Service;
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
public class TestDataSetupV2Controller {

    private final TestDataSetupV2Service testDataSetupV2Service;

    @PostMapping("/setup/v2")
    public TestDataSetupV2Response setup(
        @RequestHeader("X-Test-Setup-Token") String setupToken,
        @RequestBody TestDataSetupV2Request request
    ) {
        return testDataSetupV2Service.setup(setupToken, request);
    }
}
