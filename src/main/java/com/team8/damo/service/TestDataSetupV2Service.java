package com.team8.damo.service;

import com.team8.damo.controller.request.TestDataSetupV2Request;
import com.team8.damo.controller.response.TestDataSetupV2Response;

public interface TestDataSetupV2Service {
    TestDataSetupV2Response setup(String requestToken, TestDataSetupV2Request request);
}
