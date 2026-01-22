package com.team8.damo.controller;

import com.team8.damo.service.DiningService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class DiningControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DiningService diningService;

    @InjectMocks
    private DiningController diningController;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        mockMvc = MockMvcBuilders.standaloneSetup(diningController)
            .setValidator(new org.springframework.validation.beanvalidation.LocalValidatorFactoryBean())
            .build();
    }

    @Test
    @DisplayName("회식을 성공적으로 생성한다.")
    void createDining_success() throws Exception {
        // given
        Long groupId = 100L;
        String requestBody = """
            {
                "diningDate": "2025-12-25 18:00",
                "voteDueDate": "2025-12-20 23:59",
                "budget": 30000
            }
            """;

        given(diningService.createDining(any(), eq(groupId), any(), any())).willReturn(200L);

        // when // then
        mockMvc.perform(
                post("/api/v1/groups/{groupId}/dining", groupId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(200L));

        then(diningService).should().createDining(any(), eq(groupId), any(), any());
    }

    @Test
    @DisplayName("예산이 0원이면 회식을 생성할 수 있다.")
    void createDining_withZeroBudget() throws Exception {
        // given
        Long groupId = 100L;
        String requestBody = """
            {
                "diningDate": "2025-12-25 18:00",
                "voteDueDate": "2025-12-20 23:59",
                "budget": 0
            }
            """;

        given(diningService.createDining(any(), eq(groupId), any(), any())).willReturn(200L);

        // when // then
        mockMvc.perform(
                post("/api/v1/groups/{groupId}/dining", groupId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(200L));

        then(diningService).should().createDining(any(), eq(groupId), any(), any());
    }

    @Test
    @DisplayName("회식 날짜가 없으면 400 에러를 반환한다.")
    void createDining_diningDateRequired() throws Exception {
        // given
        Long groupId = 100L;
        String requestBody = """
            {
                "diningDate": null,
                "voteDueDate": "2025-12-20 23:59",
                "budget": 30000
            }
            """;

        // when // then
        mockMvc.perform(
                post("/api/v1/groups/{groupId}/dining", groupId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(diningService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("투표 마감 날짜가 없으면 400 에러를 반환한다.")
    void createDining_voteDueDateRequired() throws Exception {
        // given
        Long groupId = 100L;
        String requestBody = """
            {
                "diningDate": "2025-12-25 18:00",
                "voteDueDate": null,
                "budget": 30000
            }
            """;

        // when // then
        mockMvc.perform(
                post("/api/v1/groups/{groupId}/dining", groupId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(diningService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("예산이 없으면 400 에러를 반환한다.")
    void createDining_budgetRequired() throws Exception {
        // given
        Long groupId = 100L;
        String requestBody = """
            {
                "diningDate": "2025-12-25 18:00",
                "voteDueDate": "2025-12-20 23:59",
                "budget": null
            }
            """;

        // when // then
        mockMvc.perform(
                post("/api/v1/groups/{groupId}/dining", groupId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(diningService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("예산이 음수이면 400 에러를 반환한다.")
    void createDining_budgetNegative() throws Exception {
        // given
        Long groupId = 100L;
        String requestBody = """
            {
                "diningDate": "2025-12-25 18:00",
                "voteDueDate": "2025-12-20 23:59",
                "budget": -10000
            }
            """;

        // when // then
        mockMvc.perform(
                post("/api/v1/groups/{groupId}/dining", groupId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(diningService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("회식 날짜 형식이 잘못되면 400 에러를 반환한다.")
    void createDining_invalidDiningDateFormat() throws Exception {
        // given
        Long groupId = 100L;
        String requestBody = """
            {
                "diningDate": "2025/12/25 18:00",
                "voteDueDate": "2025-12-20 23:59",
                "budget": 30000
            }
            """;

        // when // then
        mockMvc.perform(
                post("/api/v1/groups/{groupId}/dining", groupId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(diningService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("투표 마감 날짜 형식이 잘못되면 400 에러를 반환한다.")
    void createDining_invalidVoteDueDateFormat() throws Exception {
        // given
        Long groupId = 100L;
        String requestBody = """
            {
                "diningDate": "2025-12-25 18:00",
                "voteDueDate": "2025/12/20 23:59",
                "budget": 30000
            }
            """;

        // when // then
        mockMvc.perform(
                post("/api/v1/groups/{groupId}/dining", groupId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(diningService).shouldHaveNoInteractions();
    }
}
