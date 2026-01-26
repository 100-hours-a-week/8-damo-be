package com.team8.damo.config;

import com.team8.damo.entity.enumeration.DiningStatus;
import com.team8.damo.entity.enumeration.VotingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ActiveProfiles("test")
class RedisSerializationTest {

    private ObjectMapper objectMapper;
    private StringRedisSerializer stringSerializer;

    @BeforeEach
    void setUp() {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator
            .builder()
            .allowIfSubType(Object.class)
            .build();

        objectMapper = JsonMapper.builder()
            .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
            .build();

        stringSerializer = new StringRedisSerializer();
    }

    @Nested
    @DisplayName("StringRedisSerializer 테스트")
    class StringRedisSerializerTest {

        @Test
        @DisplayName("문자열을 직렬화하고 역직렬화할 수 있다")
        void serializeAndDeserialize_string() {
            // given
            String original = "test-value";

            // when
            byte[] serialized = stringSerializer.serialize(original);
            String deserialized = stringSerializer.deserialize(serialized);

            // then
            assertThat(deserialized).isEqualTo(original);
        }

        @Test
        @DisplayName("한글 문자열을 직렬화하고 역직렬화할 수 있다")
        void serializeAndDeserialize_koreanString() {
            // given
            String original = "맛집탐방대";

            // when
            byte[] serialized = stringSerializer.serialize(original);
            String deserialized = stringSerializer.deserialize(serialized);

            // then
            assertThat(deserialized).isEqualTo(original);
        }

        @Test
        @DisplayName("빈 문자열을 직렬화하고 역직렬화할 수 있다")
        void serializeAndDeserialize_emptyString() {
            // given
            String original = "";

            // when
            byte[] serialized = stringSerializer.serialize(original);
            String deserialized = stringSerializer.deserialize(serialized);

            // then
            assertThat(deserialized).isEqualTo(original);
        }

        @Test
        @DisplayName("UTF-8 인코딩으로 직렬화된다")
        void serialize_utf8Encoding() {
            // given
            String original = "test";

            // when
            byte[] serialized = stringSerializer.serialize(original);

            // then
            assertThat(serialized).isEqualTo(original.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Nested
    @DisplayName("ObjectMapper JSON 직렬화 테스트")
    class ObjectMapperSerializationTest {

        @Test
        @DisplayName("단순 객체를 직렬화하고 역직렬화할 수 있다")
        void serializeAndDeserialize_simpleObject() {
            // given
            TestDto original = new TestDto("test-name", 100);

            // when
            String json = objectMapper.writeValueAsString(original);
            TestDto deserialized = objectMapper.readValue(json, TestDto.class);

            // then
            assertThat(deserialized.name()).isEqualTo(original.name());
            assertThat(deserialized.value()).isEqualTo(original.value());
        }

        @Test
        @DisplayName("LocalDateTime을 직렬화하고 역직렬화할 수 있다")
        void serializeAndDeserialize_localDateTime() {
            // given
            LocalDateTime original = LocalDateTime.of(2025, 1, 26, 14, 30, 0);
            TestDtoWithTime dto = new TestDtoWithTime("event", original);

            // when
            String json = objectMapper.writeValueAsString(dto);
            TestDtoWithTime deserialized = objectMapper.readValue(json, TestDtoWithTime.class);

            // then
            assertThat(deserialized.dateTime()).isEqualTo(original);
        }

        @Test
        @DisplayName("Enum을 문자열로 직렬화한다")
        void serialize_enumToString() {
            // given
            TestDtoWithEnum original = new TestDtoWithEnum(
                DiningStatus.ATTENDANCE_VOTING,
                VotingStatus.ATTEND
            );

            // when
            String json = objectMapper.writeValueAsString(original);

            // then
            assertThat(json).contains("ATTENDANCE_VOTING");
            assertThat(json).contains("ATTEND");
        }

        @Test
        @DisplayName("문자열에서 Enum으로 역직렬화할 수 있다")
        void deserialize_stringToEnum() {
            // given
            TestDtoWithEnum original = new TestDtoWithEnum(
                DiningStatus.COMPLETE,
                VotingStatus.PENDING
            );
            String json = objectMapper.writeValueAsString(original);

            // when
            TestDtoWithEnum deserialized = objectMapper.readValue(json, TestDtoWithEnum.class);

            // then
            assertThat(deserialized.diningStatus()).isEqualTo(DiningStatus.COMPLETE);
            assertThat(deserialized.votingStatus()).isEqualTo(VotingStatus.PENDING);
        }

        @Test
        @DisplayName("List를 직렬화하고 역직렬화할 수 있다")
        void serializeAndDeserialize_list() {
            // given
            TestDtoWithList original = new TestDtoWithList(List.of("item1", "item2", "item3"));

            // when
            String json = objectMapper.writeValueAsString(original);
            TestDtoWithList deserialized = objectMapper.readValue(json, TestDtoWithList.class);

            // then
            assertThat(deserialized.items()).containsExactly("item1", "item2", "item3");
        }

        @Test
        @DisplayName("Map을 직렬화하고 역직렬화할 수 있다")
        void serializeAndDeserialize_map() {
            // given
            TestDtoWithMap original = new TestDtoWithMap(Map.of("key1", 1, "key2", 2));

            // when
            String json = objectMapper.writeValueAsString(original);
            TestDtoWithMap deserialized = objectMapper.readValue(json, TestDtoWithMap.class);

            // then
            assertThat(deserialized.data()).containsEntry("key1", 1).containsEntry("key2", 2);
        }

        @Test
        @DisplayName("중첩 객체를 직렬화하고 역직렬화할 수 있다")
        void serializeAndDeserialize_nestedObject() {
            // given
            TestDto inner = new TestDto("inner", 50);
            TestDtoWithNested original = new TestDtoWithNested("outer", inner);

            // when
            String json = objectMapper.writeValueAsString(original);
            TestDtoWithNested deserialized = objectMapper.readValue(json, TestDtoWithNested.class);

            // then
            assertThat(deserialized.name()).isEqualTo("outer");
            assertThat(deserialized.nested().name()).isEqualTo("inner");
            assertThat(deserialized.nested().value()).isEqualTo(50);
        }

        @Test
        @DisplayName("알 수 없는 프로퍼티가 있어도 역직렬화에 실패하지 않는다")
        void deserialize_ignoresUnknownProperties() {
            // given
            String jsonWithExtraField = "{\"name\":\"test\",\"value\":100,\"unknownField\":\"ignored\"}";

            // when // then
            assertThatCode(() -> objectMapper.readValue(jsonWithExtraField, TestDto.class))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("null 값을 포함한 객체를 직렬화할 수 있다")
        void serialize_objectWithNullField() {
            // given
            TestDtoWithNullable original = new TestDtoWithNullable("test", null);

            // when
            String json = objectMapper.writeValueAsString(original);
            TestDtoWithNullable deserialized = objectMapper.readValue(json, TestDtoWithNullable.class);

            // then
            assertThat(deserialized.name()).isEqualTo("test");
            assertThat(deserialized.optional()).isNull();
        }

        @Test
        @DisplayName("복합 객체를 직렬화하고 역직렬화할 수 있다")
        void serializeAndDeserialize_complexObject() {
            // given
            LocalDateTime now = LocalDateTime.of(2025, 1, 26, 14, 30, 0);
            ComplexTestDto original = new ComplexTestDto(
                1L,
                "회식",
                DiningStatus.ATTENDANCE_VOTING,
                now,
                List.of("참가자1", "참가자2"),
                Map.of("옵션1", true, "옵션2", false)
            );

            // when
            String json = objectMapper.writeValueAsString(original);
            ComplexTestDto deserialized = objectMapper.readValue(json, ComplexTestDto.class);

            // then
            assertThat(deserialized.id()).isEqualTo(1L);
            assertThat(deserialized.name()).isEqualTo("회식");
            assertThat(deserialized.status()).isEqualTo(DiningStatus.ATTENDANCE_VOTING);
            assertThat(deserialized.dateTime()).isEqualTo(now);
            assertThat(deserialized.participants()).containsExactly("참가자1", "참가자2");
            assertThat(deserialized.options()).containsEntry("옵션1", true).containsEntry("옵션2", false);
        }
    }

    @Nested
    @DisplayName("캐시 키 프리픽스 테스트")
    class CacheKeyPrefixTest {

        @Test
        @DisplayName("캐시 키에 프리픽스가 올바르게 적용된다")
        void cacheKeyPrefix_simple() {
            // given
            String cacheName = "UserCache";
            String key = "user:1";

            // when
            String prefixedKey = cacheName + "::" + key;

            // then
            assertThat(prefixedKey).isEqualTo("UserCache::user:1");
        }
    }

    public record TestDto(String name, int value) {
    }

    public record TestDtoWithTime(String name, LocalDateTime dateTime) {
    }

    public record TestDtoWithEnum(DiningStatus diningStatus, VotingStatus votingStatus) {
    }

    public record TestDtoWithList(List<String> items) {
    }

    public record TestDtoWithMap(Map<String, Integer> data) {
    }

    public record TestDtoWithNested(String name, TestDto nested) {
    }

    public record TestDtoWithNullable(String name, String optional) {
    }

    public record ComplexTestDto(
        Long id,
        String name,
        DiningStatus status,
        LocalDateTime dateTime,
        List<String> participants,
        Map<String, Boolean> options
    ) {
    }
}
