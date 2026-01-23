package com.team8.damo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team8.damo.controller.request.PresignedUrlRequest;
import com.team8.damo.service.S3Service;
import com.team8.damo.service.response.PresignedUrlResponse;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class S3ControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private S3Controller s3Controller;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        objectMapper = new ObjectMapper();

        mockMvc = MockMvcBuilders.standaloneSetup(s3Controller)
            .setValidator(new org.springframework.validation.beanvalidation.LocalValidatorFactoryBean())
            .build();
    }

    @Test
    @DisplayName("Presigned URL을 성공적으로 발급한다.")
    void getPresignedUrl_success() throws Exception {
        // given
        PresignedUrlRequest request = createRequest("profile.jpg", "image/jpeg", "user/profile");

        PresignedUrlResponse response = new PresignedUrlResponse(
            "https://bucket.s3.amazonaws.com/presigned-url",
            "user/profile/1234567890_profile.jpg",
            300
        );

        given(s3Service.generatePresignedUrl(any())).willReturn(response);

        // when // then
        mockMvc.perform(
                put("/api/v1/s3/presigned-url")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.presignedUrl").value("https://bucket.s3.amazonaws.com/presigned-url"))
            .andExpect(jsonPath("$.data.objectKey").value("user/profile/1234567890_profile.jpg"))
            .andExpect(jsonPath("$.data.expiresIn").value(300));

        then(s3Service).should().generatePresignedUrl(any());
    }

    @Test
    @DisplayName("디렉토리 없이 Presigned URL을 발급한다.")
    void getPresignedUrl_withoutDirectory() throws Exception {
        // given
        PresignedUrlRequest request = createRequest("profile.jpg", "image/jpeg", null);

        PresignedUrlResponse response = new PresignedUrlResponse(
            "https://bucket.s3.amazonaws.com/presigned-url",
            "1234567890_profile.jpg",
            300
        );

        given(s3Service.generatePresignedUrl(any())).willReturn(response);

        // when // then
        mockMvc.perform(
                put("/api/v1/s3/presigned-url")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.objectKey").value("1234567890_profile.jpg"));

        then(s3Service).should().generatePresignedUrl(any());
    }

    @Test
    @DisplayName("파일명이 비어있으면 400 에러를 반환한다.")
    void getPresignedUrl_fileNameBlank() throws Exception {
        // given
        PresignedUrlRequest request = createRequest("", "image/jpeg", "profiles");

        // when // then
        mockMvc.perform(
                put("/api/v1/s3/presigned-url")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(s3Service).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("파일명이 null이면 400 에러를 반환한다.")
    void getPresignedUrl_fileNameNull() throws Exception {
        // given
        PresignedUrlRequest request = createRequest(null, "image/jpeg", "profiles");

        // when // then
        mockMvc.perform(
                put("/api/v1/s3/presigned-url")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(s3Service).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Content-Type이 비어있으면 400 에러를 반환한다.")
    void getPresignedUrl_contentTypeBlank() throws Exception {
        // given
        PresignedUrlRequest request = createRequest("profile.jpg", "", "profiles");

        // when // then
        mockMvc.perform(
                put("/api/v1/s3/presigned-url")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(s3Service).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Content-Type이 null이면 400 에러를 반환한다.")
    void getPresignedUrl_contentTypeNull() throws Exception {
        // given
        PresignedUrlRequest request = createRequest("profile.jpg", null, "profiles");

        // when // then
        mockMvc.perform(
                put("/api/v1/s3/presigned-url")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(s3Service).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/png", "image/jpeg", "image/jpg", "image/webp"})
    @DisplayName("허용된 Content-Type으로 요청하면 성공한다.")
    void getPresignedUrl_allowedContentTypes(String contentType) throws Exception {
        // given
        PresignedUrlRequest request = createRequest("test.file", contentType, null);

        PresignedUrlResponse response = new PresignedUrlResponse(
            "https://bucket.s3.amazonaws.com/presigned-url",
            "1234567890_test.file",
            300
        );

        given(s3Service.generatePresignedUrl(any())).willReturn(response);

        // when // then
        mockMvc.perform(
                put("/api/v1/s3/presigned-url")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andDo(print())
            .andExpect(status().isOk());

        then(s3Service).should().generatePresignedUrl(any());
    }

    private PresignedUrlRequest createRequest(String fileName, String contentType, String directory) {
        PresignedUrlRequest request = new PresignedUrlRequest();
        ReflectionTestUtils.setField(request, "fileName", fileName);
        ReflectionTestUtils.setField(request, "contentType", contentType);
        ReflectionTestUtils.setField(request, "directory", directory);
        return request;
    }
}
