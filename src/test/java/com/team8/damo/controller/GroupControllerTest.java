package com.team8.damo.controller;

import com.team8.damo.service.GroupService;
import com.team8.damo.service.response.UserGroupResponse;
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
class GroupControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GroupService groupService;

    @InjectMocks
    private GroupController groupController;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        mockMvc = MockMvcBuilders.standaloneSetup(groupController)
            .setValidator(new org.springframework.validation.beanvalidation.LocalValidatorFactoryBean())
            .build();
    }

    @Test
    @DisplayName("그룹을 성공적으로 생성한다.")
    void createGroup_success() throws Exception {
        // given
        String requestBody = """
            {
                "name": "맛집탐방대",
                "introduction": "서울 맛집을 함께 다니는 모임",
                "latitude": 37.5665,
                "longitude": 126.9780
            }
            """;

        given(groupService.createGroup(any(), any())).willReturn(100L);

        // when // then
        mockMvc.perform(
                post("/api/v1/groups")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(100L));

        then(groupService).should().createGroup(any(), any());
    }

    @Test
    @DisplayName("소개글 없이 그룹을 생성할 수 있다.")
    void createGroup_withoutIntroduction() throws Exception {
        // given
        String requestBody = """
            {
                "name": "맛집탐방대",
                "introduction": null,
                "latitude": 37.5665,
                "longitude": 126.9780
            }
            """;

        given(groupService.createGroup(any(), any())).willReturn(100L);

        // when // then
        mockMvc.perform(
                post("/api/v1/groups")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isOk());

        then(groupService).should().createGroup(any(), any());
    }

    @Test
    @DisplayName("그룹명이 비어있으면 400 에러를 반환한다.")
    void createGroup_nameBlank() throws Exception {
        // given
        String requestBody = """
            {
                "name": "",
                "introduction": "소개글",
                "latitude": 37.5665,
                "longitude": 126.9780
            }
            """;

        // when // then
        mockMvc.perform(
                post("/api/v1/groups")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(groupService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("그룹명이 2자 미만이면 400 에러를 반환한다.")
    void createGroup_nameTooShort() throws Exception {
        // given
        String requestBody = """
            {
                "name": "가",
                "introduction": "소개글",
                "latitude": 37.5665,
                "longitude": 126.9780
            }
            """;

        // when // then
        mockMvc.perform(
                post("/api/v1/groups")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(groupService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("그룹명이 10자를 초과하면 400 에러를 반환한다.")
    void createGroup_nameTooLong() throws Exception {
        // given
        String requestBody = """
            {
                "name": "가나다라마바사아자차카",
                "introduction": "소개글",
                "latitude": 37.5665,
                "longitude": 126.9780
            }
            """;

        // when // then
        mockMvc.perform(
                post("/api/v1/groups")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(groupService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("그룹명이 정확히 2자면 성공한다.")
    void createGroup_nameExactly2() throws Exception {
        // given
        String requestBody = """
            {
                "name": "가나",
                "introduction": "소개글",
                "latitude": 37.5665,
                "longitude": 126.9780
            }
            """;

        given(groupService.createGroup(any(), any())).willReturn(100L);

        // when // then
        mockMvc.perform(
                post("/api/v1/groups")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isOk());

        then(groupService).should().createGroup(any(), any());
    }

    @Test
    @DisplayName("그룹명이 정확히 10자면 성공한다.")
    void createGroup_nameExactly10() throws Exception {
        // given
        String requestBody = """
            {
                "name": "가나다라마바사아자차",
                "introduction": "소개글",
                "latitude": 37.5665,
                "longitude": 126.9780
            }
            """;

        given(groupService.createGroup(any(), any())).willReturn(100L);

        // when // then
        mockMvc.perform(
                post("/api/v1/groups")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isOk());

        then(groupService).should().createGroup(any(), any());
    }

    @Test
    @DisplayName("소개글이 30자를 초과하면 400 에러를 반환한다.")
    void createGroup_introductionTooLong() throws Exception {
        // given
        String longIntro = "가".repeat(31);
        String requestBody = """
            {
                "name": "맛집탐방대",
                "introduction": "%s",
                "latitude": 37.5665,
                "longitude": 126.9780
            }
            """.formatted(longIntro);

        // when // then
        mockMvc.perform(
                post("/api/v1/groups")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(groupService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("소개글이 정확히 30자면 성공한다.")
    void createGroup_introductionExactly30() throws Exception {
        // given
        String exactIntro = "가".repeat(30);
        String requestBody = """
            {
                "name": "맛집탐방대",
                "introduction": "%s",
                "latitude": 37.5665,
                "longitude": 126.9780
            }
            """.formatted(exactIntro);

        given(groupService.createGroup(any(), any())).willReturn(100L);

        // when // then
        mockMvc.perform(
                post("/api/v1/groups")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isOk());

        then(groupService).should().createGroup(any(), any());
    }

    @Test
    @DisplayName("사용자가 속한 그룹 목록을 성공적으로 조회한다.")
    void getGroupList_success() throws Exception {
        // given
        List<UserGroupResponse> response = List.of(
            new UserGroupResponse(100L, "맛집탐방대", "서울 맛집 모임"),
            new UserGroupResponse(101L, "카페투어", "카페 탐방 모임")
        );

        given(groupService.getGroupList(any())).willReturn(response);

        // when // then
        mockMvc.perform(
                get("/api/v1/users/me/groups")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].groupId").value(100L))
            .andExpect(jsonPath("$.data[0].name").value("맛집탐방대"))
            .andExpect(jsonPath("$.data[0].introduction").value("서울 맛집 모임"))
            .andExpect(jsonPath("$.data[1].groupId").value(101L))
            .andExpect(jsonPath("$.data[1].name").value("카페투어"));

        then(groupService).should().getGroupList(any());
    }

    @Test
    @DisplayName("속한 그룹이 없으면 빈 목록을 반환한다.")
    void getGroupList_emptyList() throws Exception {
        // given
        given(groupService.getGroupList(any())).willReturn(List.of());

        // when // then
        mockMvc.perform(
                get("/api/v1/users/me/groups")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(0));

        then(groupService).should().getGroupList(any());
    }
}
