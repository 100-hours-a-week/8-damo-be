package com.team8.damo.controller;

import com.team8.damo.entity.enumeration.AttendanceVoteStatus;
import com.team8.damo.entity.enumeration.DiningStatus;
import com.team8.damo.service.DiningService;
import com.team8.damo.service.response.AttendanceVoteDetailResponse;
import com.team8.damo.service.response.DiningConfirmedResponse;
import com.team8.damo.service.response.DiningDetailResponse;
import com.team8.damo.service.response.DiningParticipantResponse;
import com.team8.damo.service.response.DiningResponse;
import com.team8.damo.service.response.RestaurantVoteDetailResponse;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    @Test
    @DisplayName("회식 상태별 목록을 성공적으로 조회한다.")
    void getDiningList_success() throws Exception {
        // given
        Long groupId = 100L;
        DiningStatus status = DiningStatus.ATTENDANCE_VOTING;

        List<DiningResponse> responses = List.of(
            new DiningResponse(200L, LocalDateTime.of(2025, 12, 25, 18, 0), status, 3L),
            new DiningResponse(201L, LocalDateTime.of(2025, 12, 30, 19, 0), status, 5L)
        );

        given(diningService.getDiningList(any(), eq(groupId), eq(status))).willReturn(responses);

        // when // then
        mockMvc.perform(
                get("/api/v1/groups/{groupId}/dining", groupId)
                    .param("status", "ATTENDANCE_VOTING")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].diningId").value(200))
            .andExpect(jsonPath("$.data[0].status").value("ATTENDANCE_VOTING"))
            .andExpect(jsonPath("$.data[0].diningParticipantsCount").value(3))
            .andExpect(jsonPath("$.data[1].diningId").value(201))
            .andExpect(jsonPath("$.data[1].diningParticipantsCount").value(5));

        then(diningService).should().getDiningList(any(), eq(groupId), eq(status));
    }

    @Test
    @DisplayName("빈 회식 목록을 조회할 수 있다.")
    void getDiningList_emptyList() throws Exception {
        // given
        Long groupId = 100L;
        DiningStatus status = DiningStatus.COMPLETE;

        given(diningService.getDiningList(any(), eq(groupId), eq(status))).willReturn(List.of());

        // when // then
        mockMvc.perform(
                get("/api/v1/groups/{groupId}/dining", groupId)
                    .param("status", "COMPLETE")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(0));

        then(diningService).should().getDiningList(any(), eq(groupId), eq(status));
    }

    @Test
    @DisplayName("status 파라미터가 없으면 400 에러를 반환한다.")
    void getDiningList_statusRequired() throws Exception {
        // given
        Long groupId = 100L;

        // when // then
        mockMvc.perform(
                get("/api/v1/groups/{groupId}/dining", groupId)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(diningService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("유효하지 않은 status 값이면 400 에러를 반환한다.")
    void getDiningList_invalidStatus() throws Exception {
        // given
        Long groupId = 100L;

        // when // then
        mockMvc.perform(
                get("/api/v1/groups/{groupId}/dining", groupId)
                    .param("status", "INVALID_STATUS")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(diningService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("RESTAURANT_VOTING 상태로 회식 목록을 조회할 수 있다.")
    void getDiningList_restaurantVotingStatus() throws Exception {
        // given
        Long groupId = 100L;
        DiningStatus status = DiningStatus.RESTAURANT_VOTING;

        List<DiningResponse> responses = List.of(
            new DiningResponse(200L, LocalDateTime.of(2025, 12, 25, 18, 0), status, 7L)
        );

        given(diningService.getDiningList(any(), eq(groupId), eq(status))).willReturn(responses);

        // when // then
        mockMvc.perform(
                get("/api/v1/groups/{groupId}/dining", groupId)
                    .param("status", "RESTAURANT_VOTING")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].status").value("RESTAURANT_VOTING"));

        then(diningService).should().getDiningList(any(), eq(groupId), eq(status));
    }

    @Test
    @DisplayName("CONFIRMED 상태로 회식 목록을 조회할 수 있다.")
    void getDiningList_confirmedStatus() throws Exception {
        // given
        Long groupId = 100L;
        DiningStatus status = DiningStatus.CONFIRMED;

        List<DiningResponse> responses = List.of(
            new DiningResponse(200L, LocalDateTime.of(2025, 12, 25, 18, 0), status, 10L)
        );

        given(diningService.getDiningList(any(), eq(groupId), eq(status))).willReturn(responses);

        // when // then
        mockMvc.perform(
                get("/api/v1/groups/{groupId}/dining", groupId)
                    .param("status", "CONFIRMED")
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].status").value("CONFIRMED"));

        then(diningService).should().getDiningList(any(), eq(groupId), eq(status));
    }

    @Test
    @DisplayName("참석 투표를 성공적으로 한다.")
    void voteAttendance_attend_success() throws Exception {
        // given
        Long groupId = 100L;
        Long diningId = 200L;
        String requestBody = """
            {
                "attendanceVoteStatus": "ATTEND"
            }
            """;

        given(diningService.voteAttendance(any(), eq(groupId), eq(diningId), eq(AttendanceVoteStatus.ATTEND)))
            .willReturn(AttendanceVoteStatus.ATTEND);

        // when // then
        mockMvc.perform(
                patch("/api/v1/groups/{groupId}/dining/{diningId}/attendance-vote", groupId, diningId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("ATTEND"));

        then(diningService).should().voteAttendance(any(), eq(groupId), eq(diningId), eq(AttendanceVoteStatus.ATTEND));
    }

    @Test
    @DisplayName("불참 투표를 성공적으로 한다.")
    void voteAttendance_nonAttend_success() throws Exception {
        // given
        Long groupId = 100L;
        Long diningId = 200L;
        String requestBody = """
            {
                "attendanceVoteStatus": "NON_ATTEND"
            }
            """;

        given(diningService.voteAttendance(any(), eq(groupId), eq(diningId), eq(AttendanceVoteStatus.NON_ATTEND)))
            .willReturn(AttendanceVoteStatus.NON_ATTEND);

        // when // then
        mockMvc.perform(
                patch("/api/v1/groups/{groupId}/dining/{diningId}/attendance-vote", groupId, diningId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("NON_ATTEND"));

        then(diningService).should().voteAttendance(any(), eq(groupId), eq(diningId), eq(AttendanceVoteStatus.NON_ATTEND));
    }

    @Test
    @DisplayName("투표 상태가 없으면 400 에러를 반환한다.")
    void voteAttendance_votingStatusRequired() throws Exception {
        // given
        Long groupId = 100L;
        Long diningId = 200L;
        String requestBody = """
            {
                "attendanceVoteStatus": null
            }
            """;

        // when // then
        mockMvc.perform(
                patch("/api/v1/groups/{groupId}/dining/{diningId}/attendance-vote", groupId, diningId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(diningService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("PENDING 상태로는 투표할 수 없다.")
    void voteAttendance_pendingNotAllowed() throws Exception {
        // given
        Long groupId = 100L;
        Long diningId = 200L;
        String requestBody = """
            {
                "attendanceVoteStatus": "PENDING"
            }
            """;

        // when // then
        mockMvc.perform(
                patch("/api/v1/groups/{groupId}/dining/{diningId}/attendance-vote", groupId, diningId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(diningService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("유효하지 않은 투표 상태이면 400 에러를 반환한다.")
    void voteAttendance_invalidVotingStatus() throws Exception {
        // given
        Long groupId = 100L;
        Long diningId = 200L;
        String requestBody = """
            {
                "attendanceVoteStatus": "INVALID_STATUS"
            }
            """;

        // when // then
        mockMvc.perform(
                patch("/api/v1/groups/{groupId}/dining/{diningId}/attendance-vote", groupId, diningId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(diningService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("요청 본문이 비어있으면 400 에러를 반환한다.")
    void voteAttendance_emptyBody() throws Exception {
        // given
        Long groupId = 100L;
        Long diningId = 200L;
        String requestBody = "{}";

        // when // then
        mockMvc.perform(
                patch("/api/v1/groups/{groupId}/dining/{diningId}/attendance-vote", groupId, diningId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
            )
            .andDo(print())
            .andExpect(status().isBadRequest());

        then(diningService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("회식 상세를 성공적으로 조회한다.")
    void getDiningDetail_success() throws Exception {
        // given
        Long groupId = 100L;
        Long diningId = 200L;

        DiningDetailResponse response = new DiningDetailResponse(
            true,
            LocalDateTime.of(2025, 12, 25, 18, 0),
            DiningStatus.RESTAURANT_VOTING,
            List.of(
                new DiningParticipantResponse(1L, "김철수", "https://example.com/image1.jpg"),
                new DiningParticipantResponse(2L, "이영희", "https://example.com/image2.jpg")
            )
        );

        given(diningService.getDiningDetail(any(), eq(groupId), eq(diningId))).willReturn(response);

        // when // then
        mockMvc.perform(
                get("/api/v1/groups/{groupId}/dining/{diningId}", groupId, diningId)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.isGroupLeader").value(true))
            .andExpect(jsonPath("$.data.diningDate").value("2025-12-25 18:00"))
            .andExpect(jsonPath("$.data.diningStatus").value("RESTAURANT_VOTING"))
            .andExpect(jsonPath("$.data.diningParticipants").isArray())
            .andExpect(jsonPath("$.data.diningParticipants.length()").value(2))
            .andExpect(jsonPath("$.data.diningParticipants[0].userId").value(1))
            .andExpect(jsonPath("$.data.diningParticipants[0].nickname").value("김철수"));

        then(diningService).should().getDiningDetail(any(), eq(groupId), eq(diningId));
    }

    @Test
    @DisplayName("참석자가 없는 회식 상세를 조회한다.")
    void getDiningDetail_withEmptyParticipants() throws Exception {
        // given
        Long groupId = 100L;
        Long diningId = 200L;

        DiningDetailResponse response = new DiningDetailResponse(
            false,
            LocalDateTime.of(2025, 12, 25, 18, 0),
            DiningStatus.ATTENDANCE_VOTING,
            List.of()
        );

        given(diningService.getDiningDetail(any(), eq(groupId), eq(diningId))).willReturn(response);

        // when // then
        mockMvc.perform(
                get("/api/v1/groups/{groupId}/dining/{diningId}", groupId, diningId)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.isGroupLeader").value(false))
            .andExpect(jsonPath("$.data.diningStatus").value("ATTENDANCE_VOTING"))
            .andExpect(jsonPath("$.data.diningParticipants").isArray())
            .andExpect(jsonPath("$.data.diningParticipants.length()").value(0));

        then(diningService).should().getDiningDetail(any(), eq(groupId), eq(diningId));
    }

    @Test
    @DisplayName("회식 참석/불참석 투표 현황을 성공적으로 조회한다.")
    void getAttendanceVoteDetail_success() throws Exception {
        // given
        Long groupId = 100L;
        Long diningId = 200L;

        AttendanceVoteDetailResponse response = new AttendanceVoteDetailResponse(
            AttendanceVoteStatus.PENDING,
            3,
            10
        );

        given(diningService.getAttendanceVoteDetail(any(), eq(groupId), eq(diningId))).willReturn(response);

        // when // then
        mockMvc.perform(
                get("/api/v1/groups/{groupId}/dining/{diningId}/attendance-vote", groupId, diningId)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.attendanceVoteStatus").value("PENDING"))
            .andExpect(jsonPath("$.data.completedVoteCount").value(3))
            .andExpect(jsonPath("$.data.totalGroupMemberCount").value(10));

        then(diningService).should().getAttendanceVoteDetail(any(), eq(groupId), eq(diningId));
    }

    @Test
    @DisplayName("참석 투표를 완료한 사용자의 투표 현황을 조회한다.")
    void getAttendanceVoteDetail_withAttendStatus() throws Exception {
        // given
        Long groupId = 100L;
        Long diningId = 200L;

        AttendanceVoteDetailResponse response = new AttendanceVoteDetailResponse(
            AttendanceVoteStatus.ATTEND,
            5,
            10
        );

        given(diningService.getAttendanceVoteDetail(any(), eq(groupId), eq(diningId))).willReturn(response);

        // when // then
        mockMvc.perform(
                get("/api/v1/groups/{groupId}/dining/{diningId}/attendance-vote", groupId, diningId)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.attendanceVoteStatus").value("ATTEND"))
            .andExpect(jsonPath("$.data.completedVoteCount").value(5))
            .andExpect(jsonPath("$.data.totalGroupMemberCount").value(10));

        then(diningService).should().getAttendanceVoteDetail(any(), eq(groupId), eq(diningId));
    }

    @Test
    @DisplayName("불참 투표를 완료한 사용자의 투표 현황을 조회한다.")
    void getAttendanceVoteDetail_withNonAttendStatus() throws Exception {
        // given
        Long groupId = 100L;
        Long diningId = 200L;

        AttendanceVoteDetailResponse response = new AttendanceVoteDetailResponse(
            AttendanceVoteStatus.NON_ATTEND,
            7,
            10
        );

        given(diningService.getAttendanceVoteDetail(any(), eq(groupId), eq(diningId))).willReturn(response);

        // when // then
        mockMvc.perform(
                get("/api/v1/groups/{groupId}/dining/{diningId}/attendance-vote", groupId, diningId)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.attendanceVoteStatus").value("NON_ATTEND"))
            .andExpect(jsonPath("$.data.completedVoteCount").value(7))
            .andExpect(jsonPath("$.data.totalGroupMemberCount").value(10));

        then(diningService).should().getAttendanceVoteDetail(any(), eq(groupId), eq(diningId));
    }

    @Test
    @DisplayName("모든 그룹원이 투표를 완료한 경우의 투표 현황을 조회한다.")
    void getAttendanceVoteDetail_allVotesCompleted() throws Exception {
        // given
        Long groupId = 100L;
        Long diningId = 200L;

        AttendanceVoteDetailResponse response = new AttendanceVoteDetailResponse(
            AttendanceVoteStatus.ATTEND,
            10,
            10
        );

        given(diningService.getAttendanceVoteDetail(any(), eq(groupId), eq(diningId))).willReturn(response);

        // when // then
        mockMvc.perform(
                get("/api/v1/groups/{groupId}/dining/{diningId}/attendance-vote", groupId, diningId)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.attendanceVoteStatus").value("ATTEND"))
            .andExpect(jsonPath("$.data.completedVoteCount").value(10))
            .andExpect(jsonPath("$.data.totalGroupMemberCount").value(10));

        then(diningService).should().getAttendanceVoteDetail(any(), eq(groupId), eq(diningId));
    }

    @Test
    @DisplayName("확정된 회식 장소를 조회한다.")
    void getDiningConfirmed_success() throws Exception {
        // given
        Long groupId = 100L;
        Long diningId = 200L;

        DiningConfirmedResponse response = new DiningConfirmedResponse(
            500L,
            "확정된 맛집",
            "AI가 추천한 최고의 식당입니다.",
            "02-1234-5678",
            "37.5012",
            "127.0396"
        );

        given(diningService.getDiningConfirmed(any(), eq(groupId), eq(diningId))).willReturn(response);

        // when // then
        mockMvc.perform(
                get("/api/v1/groups/{groupId}/dining/{diningId}/confirmed", groupId, diningId)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.recommendRestaurantsId").value(500L))
            .andExpect(jsonPath("$.data.restaurantsName").value("확정된 맛집"))
            .andExpect(jsonPath("$.data.reasoningDescription").value("AI가 추천한 최고의 식당입니다."))
            .andExpect(jsonPath("$.data.phoneNumber").value("02-1234-5678"))
            .andExpect(jsonPath("$.data.latitude").value("37.5012"))
            .andExpect(jsonPath("$.data.longitude").value("127.0396"));

        then(diningService).should().getDiningConfirmed(any(), eq(groupId), eq(diningId));
    }

    @Test
    @DisplayName("회식 장소를 확정한다.")
    void confirmDiningRestaurant_success() throws Exception {
        // given
        Long groupId = 100L;
        Long diningId = 200L;
        Long recommendRestaurantsId = 500L;

        DiningConfirmedResponse response = new DiningConfirmedResponse(
            recommendRestaurantsId,
            "확정할 맛집",
            "AI가 추천한 식당입니다.",
            "02-1234-5678",
            "37.5012",
            "127.0396"
        );

        given(diningService.confirmDiningRestaurant(any(), eq(groupId), eq(diningId), eq(recommendRestaurantsId)))
            .willReturn(response);

        // when // then
        mockMvc.perform(
                patch("/api/v1/groups/{groupId}/dining/{diningId}/recommend-restaurants/{recommendRestaurantsId}/confirmed",
                    groupId, diningId, recommendRestaurantsId)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.recommendRestaurantsId").value(recommendRestaurantsId))
            .andExpect(jsonPath("$.data.restaurantsName").value("확정할 맛집"))
            .andExpect(jsonPath("$.data.reasoningDescription").value("AI가 추천한 식당입니다."))
            .andExpect(jsonPath("$.data.phoneNumber").value("02-1234-5678"))
            .andExpect(jsonPath("$.data.latitude").value("37.5012"))
            .andExpect(jsonPath("$.data.longitude").value("127.0396"));

        then(diningService).should().confirmDiningRestaurant(any(), eq(groupId), eq(diningId), eq(recommendRestaurantsId));
    }

    @Test
    @DisplayName("그룹장이 추천 장소를 새로고침한다.")
    void refreshRecommendRestaurants_success() throws Exception {
        // given
        Long groupId = 100L;
        Long diningId = 200L;

        List<RestaurantVoteDetailResponse> response = List.of(
            new RestaurantVoteDetailResponse(
                400L,
                "새로운 고기집",
                "AI가 추천한 식당입니다.",
                "NONE",
                "02-1234-5678",
                "37.5012",
                "127.0396",
                0,
                0
            ),
            new RestaurantVoteDetailResponse(
                401L,
                "새로운 해산물집",
                "AI가 추천한 식당입니다.",
                "NONE",
                "02-5678-1234",
                "37.5065",
                "127.0523",
                0,
                0
            )
        );

        given(diningService.refreshRecommendRestaurants(any(), eq(groupId), eq(diningId)))
            .willReturn(response);

        // when // then
        mockMvc.perform(
                post("/api/v1/groups/{groupId}/dining/{diningId}/recommend-restaurant/refresh",
                    groupId, diningId)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].recommendRestaurantsId").value(400L))
            .andExpect(jsonPath("$.data[0].restaurantsName").value("새로운 고기집"))
            .andExpect(jsonPath("$.data[0].restaurantVoteStatus").value("NONE"))
            .andExpect(jsonPath("$.data[0].likeCount").value(0))
            .andExpect(jsonPath("$.data[0].dislikeCount").value(0))
            .andExpect(jsonPath("$.data[1].recommendRestaurantsId").value(401L))
            .andExpect(jsonPath("$.data[1].restaurantsName").value("새로운 해산물집"));

        then(diningService).should().refreshRecommendRestaurants(any(), eq(groupId), eq(diningId));
    }

    @Test
    @DisplayName("추천 장소 새로고침 시 빈 목록이 반환될 수 있다.")
    void refreshRecommendRestaurants_emptyList() throws Exception {
        // given
        Long groupId = 100L;
        Long diningId = 200L;

        given(diningService.refreshRecommendRestaurants(any(), eq(groupId), eq(diningId)))
            .willReturn(List.of());

        // when // then
        mockMvc.perform(
                post("/api/v1/groups/{groupId}/dining/{diningId}/recommend-restaurant/refresh",
                    groupId, diningId)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(0));

        then(diningService).should().refreshRecommendRestaurants(any(), eq(groupId), eq(diningId));
    }
}
