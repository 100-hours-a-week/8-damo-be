package com.team8.damo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team8.damo.service.UserService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        mockMvc = MockMvcBuilders.standaloneSetup(userController)
            .setValidator(new org.springframework.validation.beanvalidation.LocalValidatorFactoryBean())
            .build();
    }

    @Test
    @DisplayName("사용자 기본 정보를 업데이트한다.")
    void updateBasic_success() throws Exception {
        // given
        String requestBody = """
            {
                "nickname": "맛집탐험가",
                "gender": "MALE",
                "ageGroup": "TWENTIES"
            }
            """;

        willDoNothing().given(userService).updateUserBasic(any(), any());

        // when // then
        mockMvc.perform(
                patch("/api/v1/users/me/basic")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isNoContent());

        then(userService).should().updateUserBasic(any(), any());
    }

    @Test
    @DisplayName("닉네임이 비어있으면 400 에러를 반환한다.")
    void updateBasic_nicknameBlank() throws Exception {
        // given
        String requestBody = """
            {
                "nickname": "",
                "gender": "MALE",
                "ageGroup": "TWENTIES"
            }
            """;

        // when // then
        mockMvc.perform(
                patch("/api/v1/users/me/basic")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(userService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("닉네임이 10자를 초과하면 400 에러를 반환한다.")
    void updateBasic_nicknameTooLong() throws Exception {
        // given
        String requestBody = """
            {
                "nickname": "가나다라마바사아자차카",
                "gender": "MALE",
                "ageGroup": "TWENTIES"
            }
            """;

        // when // then
        mockMvc.perform(
                patch("/api/v1/users/me/basic")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(userService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("닉네임에 특수문자가 포함되면 400 에러를 반환한다.")
    void updateBasic_nicknameWithSpecialCharacter() throws Exception {
        // given
        String requestBody = """
            {
                "nickname": "닉네임!@#",
                "gender": "MALE",
                "ageGroup": "TWENTIES"
            }
            """;

        // when // then
        mockMvc.perform(
                patch("/api/v1/users/me/basic")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(userService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("닉네임에 공백이 포함되면 400 에러를 반환한다.")
    void updateBasic_nicknameWithWhitespace() throws Exception {
        // given
        String requestBody = """
            {
                "nickname": "닉네 임",
                "gender": "MALE",
                "ageGroup": "TWENTIES"
            }
            """;

        // when // then
        mockMvc.perform(
                patch("/api/v1/users/me/basic")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(userService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("성별이 null이면 400 에러를 반환한다.")
    void updateBasic_genderNull() throws Exception {
        // given
        String requestBody = """
            {
                "nickname": "맛집탐험가",
                "gender": null,
                "ageGroup": "TWENTIES"
            }
            """;

        // when // then
        mockMvc.perform(
                patch("/api/v1/users/me/basic")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(userService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("연령대가 null이면 400 에러를 반환한다.")
    void updateBasic_ageGroupNull() throws Exception {
        // given
        String requestBody = """
            {
                "nickname": "맛집탐험가",
                "gender": "MALE",
                "ageGroup": null
            }
            """;

        // when // then
        mockMvc.perform(
                patch("/api/v1/users/me/basic")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(userService).shouldHaveNoInteractions();
    }
}
