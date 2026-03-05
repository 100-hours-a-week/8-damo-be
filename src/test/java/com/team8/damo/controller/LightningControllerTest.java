package com.team8.damo.controller;

import com.team8.damo.service.LightningService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class LightningControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LightningService lightningService;

    @InjectMocks
    private LightningController lightningController;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        mockMvc = MockMvcBuilders.standaloneSetup(lightningController)
            .setValidator(new org.springframework.validation.beanvalidation.LocalValidatorFactoryBean())
            .build();
    }

    @Test
    @DisplayName("번개 모임에서 성공적으로 나간다.")
    void leaveLightning_success() throws Exception {
        // given
        Long lightningId = 100L;

        willDoNothing().given(lightningService).leaveLightning(any(), eq(lightningId));

        // when // then
        mockMvc.perform(
                delete("/api/v1/lightning/{lightningId}/users/me", lightningId)
            )
            .andDo(print())
            .andExpect(status().isOk());

        then(lightningService).should().leaveLightning(any(), eq(lightningId));
    }

    @Test
    @DisplayName("번개 모임을 성공적으로 생성한다.")
    void createLightning_success() throws Exception {
        // given
        String requestBody = """
            {
                "restaurantId": "restaurant-1",
                "maxParticipants": 4,
                "description": "같이 밥 먹어요",
                "lightningDate": "2025-01-02 18:00"
            }
            """;

        given(lightningService.createLightning(any(), any(), any())).willReturn(100L);

        // when // then
        mockMvc.perform(
                post("/api/v1/lightning")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(100L));

        then(lightningService).should().createLightning(any(), any(), any());
    }

    @Test
    @DisplayName("설명 없이 번개 모임을 생성할 수 있다.")
    void createLightning_withoutDescription() throws Exception {
        // given
        String requestBody = """
            {
                "restaurantId": "restaurant-1",
                "maxParticipants": 4,
                "description": null,
                "lightningDate": "2025-01-02 18:00"
            }
            """;

        given(lightningService.createLightning(any(), any(), any())).willReturn(100L);

        // when // then
        mockMvc.perform(
                post("/api/v1/lightning")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isOk());

        then(lightningService).should().createLightning(any(), any(), any());
    }

    @Test
    @DisplayName("식당 ID가 비어있으면 400 에러를 반환한다.")
    void createLightning_restaurantIdBlank() throws Exception {
        // given
        String requestBody = """
            {
                "restaurantId": "",
                "maxParticipants": 4,
                "description": "같이 밥 먹어요",
                "lightningDate": "2025-01-02 18:00"
            }
            """;

        // when // then
        mockMvc.perform(
                post("/api/v1/lightning")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(lightningService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("참여 인원이 2명 미만이면 400 에러를 반환한다.")
    void createLightning_maxParticipantsTooSmall() throws Exception {
        // given
        String requestBody = """
            {
                "restaurantId": "restaurant-1",
                "maxParticipants": 1,
                "description": "같이 밥 먹어요",
                "lightningDate": "2025-01-02 18:00"
            }
            """;

        // when // then
        mockMvc.perform(
                post("/api/v1/lightning")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(lightningService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("참여 인원이 8명을 초과하면 400 에러를 반환한다.")
    void createLightning_maxParticipantsTooLarge() throws Exception {
        // given
        String requestBody = """
            {
                "restaurantId": "restaurant-1",
                "maxParticipants": 9,
                "description": "같이 밥 먹어요",
                "lightningDate": "2025-01-02 18:00"
            }
            """;

        // when // then
        mockMvc.perform(
                post("/api/v1/lightning")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(lightningService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("참여 인원이 정확히 2명이면 성공한다.")
    void createLightning_maxParticipantsExactly2() throws Exception {
        // given
        String requestBody = """
            {
                "restaurantId": "restaurant-1",
                "maxParticipants": 2,
                "description": "같이 밥 먹어요",
                "lightningDate": "2025-01-02 18:00"
            }
            """;

        given(lightningService.createLightning(any(), any(), any())).willReturn(100L);

        // when // then
        mockMvc.perform(
                post("/api/v1/lightning")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isOk());

        then(lightningService).should().createLightning(any(), any(), any());
    }

    @Test
    @DisplayName("참여 인원이 정확히 8명이면 성공한다.")
    void createLightning_maxParticipantsExactly8() throws Exception {
        // given
        String requestBody = """
            {
                "restaurantId": "restaurant-1",
                "maxParticipants": 8,
                "description": "같이 밥 먹어요",
                "lightningDate": "2025-01-02 18:00"
            }
            """;

        given(lightningService.createLightning(any(), any(), any())).willReturn(100L);

        // when // then
        mockMvc.perform(
                post("/api/v1/lightning")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isOk());

        then(lightningService).should().createLightning(any(), any(), any());
    }

    @Test
    @DisplayName("설명이 30자를 초과하면 400 에러를 반환한다.")
    void createLightning_descriptionTooLong() throws Exception {
        // given
        String longDescription = "가".repeat(31);
        String requestBody = """
            {
                "restaurantId": "restaurant-1",
                "maxParticipants": 4,
                "description": "%s",
                "lightningDate": "2025-01-02 18:00"
            }
            """.formatted(longDescription);

        // when // then
        mockMvc.perform(
                post("/api/v1/lightning")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(lightningService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("설명이 정확히 30자면 성공한다.")
    void createLightning_descriptionExactly30() throws Exception {
        // given
        String exactDescription = "가".repeat(30);
        String requestBody = """
            {
                "restaurantId": "restaurant-1",
                "maxParticipants": 4,
                "description": "%s",
                "lightningDate": "2025-01-02 18:00"
            }
            """.formatted(exactDescription);

        given(lightningService.createLightning(any(), any(), any())).willReturn(100L);

        // when // then
        mockMvc.perform(
                post("/api/v1/lightning")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isOk());

        then(lightningService).should().createLightning(any(), any(), any());
    }

    @Test
    @DisplayName("번개 진행 날짜가 없으면 400 에러를 반환한다.")
    void createLightning_lightningDateNull() throws Exception {
        // given
        String requestBody = """
            {
                "restaurantId": "restaurant-1",
                "maxParticipants": 4,
                "description": "같이 밥 먹어요",
                "lightningDate": null
            }
            """;

        // when // then
        mockMvc.perform(
                post("/api/v1/lightning")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(lightningService).shouldHaveNoInteractions();
    }
}
