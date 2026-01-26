package com.team8.damo.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team8.damo.client.request.AiRecommendationRequest;
import com.team8.damo.client.request.DiningData;
import com.team8.damo.client.response.AiRecommendationResponse;
import com.team8.damo.client.response.RecommendedItem;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiClientTest {

    private MockWebServer mockWebServer;
    private AiClient aiClient;
    private AiClient realAiClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        RestClient restClient = RestClient.builder()
            .baseUrl(mockWebServer.url("/").toString())
            .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build();

        aiClient = factory.createClient(AiClient.class);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        RestClient realRestClient = RestClient.builder()
            .baseUrl("https://damo.today")
            .build();

        HttpServiceProxyFactory realFactory = HttpServiceProxyFactory.builder()
            .exchangeAdapter(RestClientAdapter.create(realRestClient))
            .build();

        realAiClient = realFactory.createClient(AiClient.class);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

//    @Test
//    @DisplayName("AI Health check 확인")
//    void aiHealthCheck() {
//        boolean health = realAiClient.health();
//        assertThat(health).isTrue();
//    }

    @Test
    @DisplayName("AI 추천 API 호출 성공")
    void recommendation_success() throws Exception {
        // given
        AiRecommendationRequest request = createSampleRequest();
        AiRecommendationResponse expectedResponse = new AiRecommendationResponse(
            2,
            List.of(
                new RecommendedItem("1", "맛있는 식당", "사용자 취향에 맞는 한식당입니다."),
                new RecommendedItem("2", "좋은 레스토랑", "예산에 적합한 양식당입니다.")
            )
        );

        mockWebServer.enqueue(new MockResponse()
            .setBody(objectMapper.writeValueAsString(expectedResponse))
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // when
        AiRecommendationResponse response = aiClient.recommendation(request);

        // then
        assertThat(response.recommendationCount()).isEqualTo(2);
        assertThat(response.recommendationCount()).isEqualTo(response.recommendedItems().size());
        assertThat(response.recommendedItems()).hasSize(2);
        assertThat(response.recommendedItems().get(0).restaurantId()).isEqualTo("1");
        assertThat(response.recommendedItems().get(0).restaurantName()).isEqualTo("맛있는 식당");
    }

    @Test
    @DisplayName("요청 본문 JSON 형식 검증")
    void recommendation_requestBodyIsCorrectlyFormatted() throws Exception {
        // given
        AiRecommendationRequest request = createSampleRequest();
        AiRecommendationResponse mockResponse = new AiRecommendationResponse(
            0,
            Collections.emptyList()
        );

        mockWebServer.enqueue(new MockResponse()
            .setBody(objectMapper.writeValueAsString(mockResponse))
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // when
        aiClient.recommendation(request);

        // then
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertThat(recordedRequest.getPath()).isEqualTo("/ai/api/v1/recommendation");
        assertThat(recordedRequest.getHeader(HttpHeaders.CONTENT_TYPE)).contains(MediaType.APPLICATION_JSON_VALUE);

        String requestBody = recordedRequest.getBody().readUtf8();
        assertThat(requestBody).contains("diningData");
        assertThat(requestBody).contains("userIds");
    }

    @Test
    @DisplayName("빈 추천 결과 처리")
    void recommendation_emptyRecommendedItems() throws Exception {
        // given
        AiRecommendationRequest request = createSampleRequest();
        AiRecommendationResponse expectedResponse = new AiRecommendationResponse(
            0,
            Collections.emptyList()
        );

        mockWebServer.enqueue(new MockResponse()
            .setBody(objectMapper.writeValueAsString(expectedResponse))
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // when
        AiRecommendationResponse response = aiClient.recommendation(request);

        // then
        assertThat(response.recommendationCount()).isZero();
        assertThat(response.recommendedItems()).isEmpty();
    }

    @Test
    @DisplayName("다수 사용자 요청 처리")
    void recommendation_multipleUsers() throws Exception {
        // given
        DiningData diningData = new DiningData(
            1L,
            100L,
            LocalDate.of(2025, 1, 30),
            50000L
        );
        List<Long> users = List.of(123L, 124L);

        AiRecommendationRequest request = new AiRecommendationRequest(diningData, users);
        AiRecommendationResponse expectedResponse = new AiRecommendationResponse(
            1,
            List.of(new RecommendedItem("5", "채식 식당", "모든 사용자의 제한사항을 고려한 식당입니다."))
        );

        mockWebServer.enqueue(new MockResponse()
            .setBody(objectMapper.writeValueAsString(expectedResponse))
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // when
        AiRecommendationResponse response = aiClient.recommendation(request);

        // then
        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        String requestBody = recordedRequest.getBody().readUtf8();

        assertThat(requestBody).contains("userIds");
        assertThat(requestBody).contains("123");
        assertThat(requestBody).contains("124");
        assertThat(response.recommendationCount()).isEqualTo(1);
        assertThat(response.recommendedItems()).hasSize(1);
    }

    @Test
    @DisplayName("서버 오류(5xx) 처리")
    void recommendation_serverError_throwsException() {
        // given
        AiRecommendationRequest request = createSampleRequest();

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .setBody("{\"error\": \"Internal Server Error\"}")
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // when & then
        assertThatThrownBy(() -> aiClient.recommendation(request))
            .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    @DisplayName("클라이언트 오류(4xx) 처리")
    void recommendation_badRequest_throwsException() {
        // given
        AiRecommendationRequest request = createSampleRequest();

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(400)
            .setBody("{\"error\": \"Bad Request\"}")
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // when & then
        assertThatThrownBy(() -> aiClient.recommendation(request))
            .isInstanceOf(HttpClientErrorException.class);
    }

    @Test
    @DisplayName("추천 결과 0건 처리")
    void recommendation_zeroCount() throws Exception {
        // given
        AiRecommendationRequest request = createSampleRequest();
        AiRecommendationResponse expectedResponse = new AiRecommendationResponse(
            0,
            Collections.emptyList()
        );

        mockWebServer.enqueue(new MockResponse()
            .setBody(objectMapper.writeValueAsString(expectedResponse))
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        // when
        AiRecommendationResponse response = aiClient.recommendation(request);

        // then
        assertThat(response.recommendationCount()).isZero();
        assertThat(response.recommendedItems()).isEmpty();
    }

    private AiRecommendationRequest createSampleRequest() {
        DiningData diningData = new DiningData(
            1L,
            100L,
            LocalDate.of(2025, 1, 30),
            30000L
        );

        return new AiRecommendationRequest(diningData, List.of(123L, 124L));
    }
}
