package com.team8.damo.service;

import com.team8.damo.controller.request.TestDataCleanupV2Request;
import com.team8.damo.controller.response.TestDataCleanupV2Response;

public interface TestDataCleanupV2Service {
    TestDataCleanupV2Response cleanup(String requestToken, TestDataCleanupV2Request request);
}
