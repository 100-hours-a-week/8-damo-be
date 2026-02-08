package com.team8.damo.controller;

import com.team8.damo.entity.User;
import com.team8.damo.entity.enumeration.*;
import com.team8.damo.fixture.CategoryFixture;
import com.team8.damo.fixture.UserFixture;
import com.team8.damo.service.UserService;
import com.team8.damo.service.response.UserBasicResponse;
import com.team8.damo.service.response.UserProfileResponse;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
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
                "ageGroup": "TWENTIES",
                "imagePath": "image"
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
            .andExpect(status().isOk());

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

    @Test
    @DisplayName("사용자 개인 특성을 성공적으로 등록한다.")
    void createCharacteristics_success() throws Exception {
        // given
        String requestBody = """
            {
                "allergies": ["SHRIMP", "CRAB"],
                "likeFoods": ["KOREAN", "CHINESE"],
                "likeIngredients": ["MEAT", "SEAFOOD"],
                "otherCharacteristics": "매운 음식을 좋아합니다"
            }
            """;

        willDoNothing().given(userService).createCharacteristics(any(), any());

        // when // then
        mockMvc.perform(
                post("/api/v1/users/me/characteristics")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isOk());

        then(userService).should().createCharacteristics(any(), any());
    }

    @Test
    @DisplayName("모든 필드가 비어있어도 특성 등록에 성공한다.")
    void createCharacteristics_emptyFields() throws Exception {
        // given
        String requestBody = """
            {
                "allergies": [],
                "likeFoods": [],
                "likeIngredients": [],
                "otherCharacteristics": null
            }
            """;

        willDoNothing().given(userService).createCharacteristics(any(), any());

        // when // then
        mockMvc.perform(
                post("/api/v1/users/me/characteristics")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isOk());

        then(userService).should().createCharacteristics(any(), any());
    }

    @Test
    @DisplayName("기타 특성이 100자를 초과하면 400 에러를 반환한다.")
    void createCharacteristics_otherCharacteristicsTooLong() throws Exception {
        // given
        String longText = "가".repeat(101);
        String requestBody = """
            {
                "allergies": ["SHRIMP"],
                "likeFoods": ["KOREAN"],
                "likeIngredients": ["MEAT"],
                "otherCharacteristics": "%s"
            }
            """.formatted(longText);

        // when // then
        mockMvc.perform(
                post("/api/v1/users/me/characteristics")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(userService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("기타 특성이 정확히 100자면 성공한다.")
    void createCharacteristics_otherCharacteristicsExactly100() throws Exception {
        // given
        String exactText = "가".repeat(100);
        String requestBody = """
            {
                "allergies": [],
                "likeFoods": [],
                "likeIngredients": [],
                "otherCharacteristics": "%s"
            }
            """.formatted(exactText);

        willDoNothing().given(userService).createCharacteristics(any(), any());

        // when // then
        mockMvc.perform(
                post("/api/v1/users/me/characteristics")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isOk());

        then(userService).should().createCharacteristics(any(), any());
    }

    @Test
    @DisplayName("사용자 프로필을 성공적으로 조회한다.")
    void getProfile_success() throws Exception {
        // given
        Long userId = 1L;
        User user = UserFixture.create(userId);
        user.updateBasic("맛집탐험가", Gender.MALE, AgeGroup.TWENTIES);

        UserProfileResponse response = UserProfileResponse.of(
            user,
            List.of(
                CategoryFixture.createAllergyCategory(1, AllergyType.SHRIMP),
                CategoryFixture.createAllergyCategory(2, AllergyType.CRAB)
            ),
            List.of(
                CategoryFixture.createLikeFoodCategory(1, FoodType.KOREAN)
            ),
            List.of(
                CategoryFixture.createLikeIngredientCategory(1, IngredientType.MEAT)
            )
        );

        given(userService.getUserProfile(any())).willReturn(response);

        // when // then
        mockMvc.perform(
                get("/api/v1/users/me/profile")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.userId").value(userId))
            .andExpect(jsonPath("$.data.nickname").value("맛집탐험가"))
            .andExpect(jsonPath("$.data.allergies").isArray())
            .andExpect(jsonPath("$.data.allergies.length()").value(2))
            .andExpect(jsonPath("$.data.allergies[0].id").value(1))
            .andExpect(jsonPath("$.data.allergies[0].category").value("새우"))
            .andExpect(jsonPath("$.data.likeFoods").isArray())
            .andExpect(jsonPath("$.data.likeFoods.length()").value(1))
            .andExpect(jsonPath("$.data.likeFoods[0].category").value("한식"))
            .andExpect(jsonPath("$.data.likeIngredients").isArray())
            .andExpect(jsonPath("$.data.likeIngredients.length()").value(1))
            .andExpect(jsonPath("$.data.likeIngredients[0].category").value("육류"));

        then(userService).should().getUserProfile(any());
    }

    @Test
    @DisplayName("카테고리가 모두 비어있어도 프로필 조회에 성공한다.")
    void getProfile_emptyCategories() throws Exception {
        // given
        Long userId = 1L;
        User user = UserFixture.create(userId);
        user.updateBasic("새회원", Gender.FEMALE, AgeGroup.THIRTIES);

        UserProfileResponse response = UserProfileResponse.of(
            user,
            List.of(),
            List.of(),
            List.of()
        );

        given(userService.getUserProfile(any())).willReturn(response);

        // when // then
        mockMvc.perform(
                get("/api/v1/users/me/profile")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.userId").value(userId))
            .andExpect(jsonPath("$.data.nickname").value("새회원"))
            .andExpect(jsonPath("$.data.allergies").isArray())
            .andExpect(jsonPath("$.data.allergies.length()").value(0))
            .andExpect(jsonPath("$.data.likeFoods").isArray())
            .andExpect(jsonPath("$.data.likeFoods.length()").value(0))
            .andExpect(jsonPath("$.data.likeIngredients").isArray())
            .andExpect(jsonPath("$.data.likeIngredients.length()").value(0));

        then(userService).should().getUserProfile(any());
    }

    @Test
    @DisplayName("사용자 기본 정보를 성공적으로 조회한다.")
    void getBasic_success() throws Exception {
        // given
        Long userId = 1L;
        User user = UserFixture.create(userId);
        user.updateBasic("맛집탐험가", Gender.FEMALE, AgeGroup.THIRTIES);

        UserBasicResponse response = UserBasicResponse.from(user);

        given(userService.getUserBasic(any())).willReturn(response);

        // when // then
        mockMvc.perform(
                get("/api/v1/users/me/basic")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.userId").value(userId))
            .andExpect(jsonPath("$.data.nickname").value("맛집탐험가"))
            .andExpect(jsonPath("$.data.gender").value("FEMALE"))
            .andExpect(jsonPath("$.data.ageGroup").value("THIRTIES"));

        then(userService).should().getUserBasic(any());
    }

    @Test
    @DisplayName("사용자 개인 특성을 성공적으로 수정한다.")
    void updateCharacteristics_success() throws Exception {
        // given
        String requestBody = """
            {
                "allergies": ["SHRIMP", "CRAB"],
                "likeFoods": ["KOREAN", "CHINESE"],
                "likeIngredients": ["MEAT", "SEAFOOD"],
                "otherCharacteristics": "매운 음식을 좋아합니다"
            }
            """;

        willDoNothing().given(userService).updateUserCharacteristics(any(), any());

        // when // then
        mockMvc.perform(
                patch("/api/v1/users/me/characteristics")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isOk());

        then(userService).should().updateUserCharacteristics(any(), any());
    }

    @Test
    @DisplayName("알레르기가 비어있어도 특성 수정에 성공한다.")
    void updateCharacteristics_emptyAllergies() throws Exception {
        // given
        String requestBody = """
            {
                "allergies": [],
                "likeFoods": ["KOREAN"],
                "likeIngredients": ["MEAT"],
                "otherCharacteristics": null
            }
            """;

        willDoNothing().given(userService).updateUserCharacteristics(any(), any());

        // when // then
        mockMvc.perform(
                patch("/api/v1/users/me/characteristics")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isOk());

        then(userService).should().updateUserCharacteristics(any(), any());
    }

    @Test
    @DisplayName("알레르기가 null이어도 특성 수정에 성공한다.")
    void updateCharacteristics_nullAllergies() throws Exception {
        // given
        String requestBody = """
            {
                "allergies": null,
                "likeFoods": ["KOREAN"],
                "likeIngredients": ["MEAT"],
                "otherCharacteristics": null
            }
            """;

        willDoNothing().given(userService).updateUserCharacteristics(any(), any());

        // when // then
        mockMvc.perform(
                patch("/api/v1/users/me/characteristics")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isOk());

        then(userService).should().updateUserCharacteristics(any(), any());
    }

    @Test
    @DisplayName("선호 음식이 비어있으면 400 에러를 반환한다.")
    void updateCharacteristics_emptyLikeFoods() throws Exception {
        // given
        String requestBody = """
            {
                "allergies": ["SHRIMP"],
                "likeFoods": [],
                "likeIngredients": ["MEAT"],
                "otherCharacteristics": null
            }
            """;

        // when // then
        mockMvc.perform(
                patch("/api/v1/users/me/characteristics")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(userService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("선호 음식이 null이면 400 에러를 반환한다.")
    void updateCharacteristics_nullLikeFoods() throws Exception {
        // given
        String requestBody = """
            {
                "allergies": ["SHRIMP"],
                "likeFoods": null,
                "likeIngredients": ["MEAT"],
                "otherCharacteristics": null
            }
            """;

        // when // then
        mockMvc.perform(
                patch("/api/v1/users/me/characteristics")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(userService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("선호 재료가 비어있으면 400 에러를 반환한다.")
    void updateCharacteristics_emptyLikeIngredients() throws Exception {
        // given
        String requestBody = """
            {
                "allergies": [],
                "likeFoods": ["KOREAN"],
                "likeIngredients": [],
                "otherCharacteristics": null
            }
            """;

        // when // then
        mockMvc.perform(
                patch("/api/v1/users/me/characteristics")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(userService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("선호 재료가 null이면 400 에러를 반환한다.")
    void updateCharacteristics_nullLikeIngredients() throws Exception {
        // given
        String requestBody = """
            {
                "allergies": [],
                "likeFoods": ["KOREAN"],
                "likeIngredients": null,
                "otherCharacteristics": null
            }
            """;

        // when // then
        mockMvc.perform(
                patch("/api/v1/users/me/characteristics")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(userService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("특성 수정 시 기타 특성이 100자를 초과하면 400 에러를 반환한다.")
    void updateCharacteristics_otherCharacteristicsTooLong() throws Exception {
        // given
        String longText = "가".repeat(101);
        String requestBody = """
            {
                "allergies": ["SHRIMP"],
                "likeFoods": ["KOREAN"],
                "likeIngredients": ["MEAT"],
                "otherCharacteristics": "%s"
            }
            """.formatted(longText);

        // when // then
        mockMvc.perform(
                patch("/api/v1/users/me/characteristics")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(userService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("특성 수정 시 기타 특성이 정확히 100자면 성공한다.")
    void updateCharacteristics_otherCharacteristicsExactly100() throws Exception {
        // given
        String exactText = "가".repeat(100);
        String requestBody = """
            {
                "allergies": [],
                "likeFoods": ["KOREAN"],
                "likeIngredients": ["MEAT"],
                "otherCharacteristics": "%s"
            }
            """.formatted(exactText);

        willDoNothing().given(userService).updateUserCharacteristics(any(), any());

        // when // then
        mockMvc.perform(
                patch("/api/v1/users/me/characteristics")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isOk());

        then(userService).should().updateUserCharacteristics(any(), any());
    }

    @Test
    @DisplayName("회원 탈퇴를 성공적으로 처리한다.")
    void withdraw_success() throws Exception {
        // given
        willDoNothing().given(userService).withdraw(any());

        // when // then
        mockMvc.perform(
                delete("/api/v1/users/me")
            )
            .andDo(print())
            .andExpect(status().isOk());

        then(userService).should().withdraw(any());
    }
}
