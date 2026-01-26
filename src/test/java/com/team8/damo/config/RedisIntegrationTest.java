package com.team8.damo.config;

import com.team8.damo.entity.enumeration.DiningStatus;
import com.team8.damo.entity.enumeration.VotingStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RedisTestConfig.class)
class RedisIntegrationTest {

    private static final String TEST_KEY_PREFIX = "test:redis:";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private CacheManager cacheManager;

    private ValueOperations<String, String> valueOps;

    @BeforeEach
    void setUp() {
        valueOps = redisTemplate.opsForValue();
    }

    @AfterEach
    void tearDown() {
        Set<String> keys = redisTemplate.keys(TEST_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        Cache testCache = cacheManager.getCache("testCache");
        if (testCache != null) {
            testCache.clear();
        }
    }

    @Nested
    @DisplayName("RedisTemplate 직렬화 테스트")
    class RedisTemplateSerializationTest {

        @Test
        @DisplayName("문자열 값을 저장하고 조회할 수 있다")
        void saveAndRetrieve_stringValue() {
            // given
            String key = TEST_KEY_PREFIX + "string";
            String value = "test-value";

            // when
            valueOps.set(key, value);
            Object retrieved = valueOps.get(key);

            // then
            assertThat(retrieved).isEqualTo(value);
        }

        @Test
        @DisplayName("한글 값을 저장하고 조회할 수 있다")
        void saveAndRetrieve_koreanValue() {
            // given
            String key = TEST_KEY_PREFIX + "korean";
            String value = "맛집탐방대 회식";

            // when
            valueOps.set(key, value);
            Object retrieved = valueOps.get(key);

            // then
            assertThat(retrieved).isEqualTo(value);
        }

        @Test
        @DisplayName("TTL을 설정하여 값을 저장할 수 있다")
        void saveWithTtl() {
            // given
            String key = TEST_KEY_PREFIX + "ttl";
            String value = "expires-soon";

            // when
            valueOps.set(key, value, 1, TimeUnit.HOURS);
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);

            // then
            assertThat(ttl).isGreaterThan(0);
            assertThat(ttl).isLessThanOrEqualTo(3600);
        }

        @Test
        @DisplayName("존재하지 않는 키를 조회하면 null을 반환한다")
        void retrieve_nonExistentKey() {
            // given
            String key = TEST_KEY_PREFIX + "non-existent";

            // when
            Object retrieved = valueOps.get(key);

            // then
            assertThat(retrieved).isNull();
        }

        @Test
        @DisplayName("키를 삭제할 수 있다")
        void deleteKey() {
            // given
            String key = TEST_KEY_PREFIX + "to-delete";
            valueOps.set(key, "value");

            // when
            Boolean deleted = redisTemplate.delete(key);
            Object retrieved = valueOps.get(key);

            // then
            assertThat(deleted).isTrue();
            assertThat(retrieved).isNull();
        }

        @Test
        @DisplayName("키 존재 여부를 확인할 수 있다")
        void checkKeyExists() {
            // given
            String key = TEST_KEY_PREFIX + "exists";
            valueOps.set(key, "value");

            // when
            Boolean exists = redisTemplate.hasKey(key);
            Boolean notExists = redisTemplate.hasKey(TEST_KEY_PREFIX + "not-exists");

            // then
            assertThat(exists).isTrue();
            assertThat(notExists).isFalse();
        }
    }

    @Nested
    @DisplayName("RedisTemplate Hash 연산 테스트")
    class RedisTemplateHashOperationsTest {

        @Test
        @DisplayName("Hash에 값을 저장하고 조회할 수 있다")
        void saveAndRetrieve_hashValue() {
            // given
            String key = TEST_KEY_PREFIX + "hash";
            String hashKey = "field1";
            String value = "hash-value";

            // when
            redisTemplate.opsForHash().put(key, hashKey, value);
            Object retrieved = redisTemplate.opsForHash().get(key, hashKey);

            // then
            assertThat(retrieved).isEqualTo(value);
        }

        @Test
        @DisplayName("Hash에 여러 필드를 저장하고 조회할 수 있다")
        void saveAndRetrieve_multipleHashFields() {
            // given
            String key = TEST_KEY_PREFIX + "multi-hash";
            Map<String, String> fields = Map.of(
                "name", "회식",
                "status", "ATTENDANCE_VOTING",
                "count", "5"
            );

            // when
            redisTemplate.opsForHash().putAll(key, fields);
            Map<Object, Object> retrieved = redisTemplate.opsForHash().entries(key);

            // then
            assertThat(retrieved)
                .containsEntry("name", "회식")
                .containsEntry("status", "ATTENDANCE_VOTING")
                .containsEntry("count", "5");
        }
    }

    @Nested
    @DisplayName("CacheManager 테스트")
    class CacheManagerTest {

        @Test
        @DisplayName("CacheManager가 정상적으로 빈 등록된다")
        void cacheManagerBeanLoaded() {
            // then
            assertThat(cacheManager).isNotNull();
        }

        @Test
        @DisplayName("캐시를 생성하고 조회할 수 있다")
        void getCache() {
            // when
            Cache cache = cacheManager.getCache("testCache");

            // then
            assertThat(cache).isNotNull();
            assertThat(cache.getName()).isEqualTo("testCache");
        }

        @Test
        @DisplayName("캐시에 primitive 값을 저장하고 조회할 수 있다")
        void saveAndRetrieve_primitiveValue() {
            // given
            Cache cache = cacheManager.getCache("testCache");
            assertThat(cache).isNotNull();

            String key = "numberKey";
            Integer value = 12345;

            // when
            cache.put(key, value);
            Cache.ValueWrapper wrapper = cache.get(key);

            // then
            assertThat(wrapper).isNotNull();
            assertThat(wrapper.get()).isEqualTo(value);
        }

    }

    @Nested
    @DisplayName("Redis 연결 테스트")
    class RedisConnectionTest {

        @Test
        @DisplayName("Redis 서버에 연결할 수 있다")
        void connectionTest() {
            // when
            String result = redisTemplate.getConnectionFactory()
                .getConnection()
                .ping();

            // then
            assertThat(result).isEqualTo("PONG");
        }
    }

    @Nested
    @DisplayName("JSON 직렬화 테스트")
    class JsonSerializationTest {

        @Test
        @DisplayName("DTO 객체를 JSON으로 직렬화하여 저장하고 조회할 수 있다")
        void saveAndRetrieve_dtoObject() {
            // given
            Cache cache = cacheManager.getCache("dtoCache");
            assertThat(cache).isNotNull();

            String key = "dining:1";
            DiningCacheDto dto = new DiningCacheDto(
                1L,
                "신년 회식",
                DiningStatus.ATTENDANCE_VOTING,
                LocalDateTime.of(2025, 1, 26, 18, 30),
                5,
                List.of("김철수", "이영희", "박민수")
            );

            // when
            cache.put(key, dto);
            Cache.ValueWrapper wrapper = cache.get(key);

            // then
            assertThat(wrapper).isNotNull();
            Object retrieved = wrapper.get();
            assertThat(retrieved).isInstanceOf(DiningCacheDto.class);

            DiningCacheDto result = (DiningCacheDto) retrieved;
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("신년 회식");
            assertThat(result.getStatus()).isEqualTo(DiningStatus.ATTENDANCE_VOTING);
            assertThat(result.getDiningDate()).isEqualTo(LocalDateTime.of(2025, 1, 26, 18, 30));
            assertThat(result.getParticipantCount()).isEqualTo(5);
            assertThat(result.getParticipants()).containsExactly("김철수", "이영희", "박민수");
        }

        @Test
        @DisplayName("Enum을 포함한 DTO를 직렬화하고 역직렬화할 수 있다")
        void saveAndRetrieve_dtoWithEnum() {
            // given
            Cache cache = cacheManager.getCache("enumCache");
            assertThat(cache).isNotNull();

            String key = "participant:1";
            ParticipantCacheDto dto = new ParticipantCacheDto(
                100L,
                "홍길동",
                VotingStatus.ATTEND,
                DiningStatus.RESTAURANT_VOTING
            );

            // when
            cache.put(key, dto);
            Cache.ValueWrapper wrapper = cache.get(key);

            // then
            assertThat(wrapper).isNotNull();
            ParticipantCacheDto result = (ParticipantCacheDto) wrapper.get();
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getName()).isEqualTo("홍길동");
            assertThat(result.getVotingStatus()).isEqualTo(VotingStatus.ATTEND);
            assertThat(result.getDiningStatus()).isEqualTo(DiningStatus.RESTAURANT_VOTING);
        }

        @Test
        @DisplayName("LocalDateTime을 포함한 DTO를 직렬화하고 역직렬화할 수 있다")
        void saveAndRetrieve_dtoWithLocalDateTime() {
            // given
            Cache cache = cacheManager.getCache("timeCache");
            assertThat(cache).isNotNull();

            String key = "event:1";
            LocalDateTime now = LocalDateTime.of(2025, 6, 15, 12, 30, 45);
            EventCacheDto dto = new EventCacheDto(
                1L,
                "프로젝트 마감",
                now,
                now.plusDays(7)
            );

            // when
            cache.put(key, dto);
            Cache.ValueWrapper wrapper = cache.get(key);

            // then
            assertThat(wrapper).isNotNull();
            EventCacheDto result = (EventCacheDto) wrapper.get();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEventName()).isEqualTo("프로젝트 마감");
            assertThat(result.getStartDate()).isEqualTo(now);
            assertThat(result.getEndDate()).isEqualTo(now.plusDays(7));
        }

        @Test
        @DisplayName("List를 포함한 DTO를 직렬화하고 역직렬화할 수 있다")
        void saveAndRetrieve_dtoWithList() {
            // given
            Cache cache = cacheManager.getCache("listCache");
            assertThat(cache).isNotNull();

            String key = "group:1";
            GroupCacheDto dto = new GroupCacheDto(
                1L,
                "맛집탐방대",
                List.of("member1@test.com", "member2@test.com", "member3@test.com"),
                Map.of("설정1", "값1", "설정2", "값2")
            );

            // when
            cache.put(key, dto);
            Cache.ValueWrapper wrapper = cache.get(key);

            // then
            assertThat(wrapper).isNotNull();
            GroupCacheDto result = (GroupCacheDto) wrapper.get();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getGroupName()).isEqualTo("맛집탐방대");
            assertThat(result.getMemberEmails()).containsExactly("member1@test.com", "member2@test.com", "member3@test.com");
            assertThat(result.getSettings())
                .containsEntry("설정1", "값1")
                .containsEntry("설정2", "값2");
        }
    }

    // Test DTO Classes (일반 클래스 - Jackson NON_FINAL 타이핑 호환)
    public static class DiningCacheDto {
        private Long id;
        private String title;
        private DiningStatus status;
        private LocalDateTime diningDate;
        private int participantCount;
        private List<String> participants;

        public DiningCacheDto() {}

        public DiningCacheDto(Long id, String title, DiningStatus status,
                              LocalDateTime diningDate, int participantCount, List<String> participants) {
            this.id = id;
            this.title = title;
            this.status = status;
            this.diningDate = diningDate;
            this.participantCount = participantCount;
            this.participants = participants;
        }

        public Long getId() { return id; }
        public String getTitle() { return title; }
        public DiningStatus getStatus() { return status; }
        public LocalDateTime getDiningDate() { return diningDate; }
        public int getParticipantCount() { return participantCount; }
        public List<String> getParticipants() { return participants; }
    }

    public static class ParticipantCacheDto {
        private Long id;
        private String name;
        private VotingStatus votingStatus;
        private DiningStatus diningStatus;

        public ParticipantCacheDto() {}

        public ParticipantCacheDto(Long id, String name, VotingStatus votingStatus, DiningStatus diningStatus) {
            this.id = id;
            this.name = name;
            this.votingStatus = votingStatus;
            this.diningStatus = diningStatus;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public VotingStatus getVotingStatus() { return votingStatus; }
        public DiningStatus getDiningStatus() { return diningStatus; }
    }

    public static class EventCacheDto {
        private Long id;
        private String eventName;
        private LocalDateTime startDate;
        private LocalDateTime endDate;

        public EventCacheDto() {}

        public EventCacheDto(Long id, String eventName, LocalDateTime startDate, LocalDateTime endDate) {
            this.id = id;
            this.eventName = eventName;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public Long getId() { return id; }
        public String getEventName() { return eventName; }
        public LocalDateTime getStartDate() { return startDate; }
        public LocalDateTime getEndDate() { return endDate; }
    }

    public static class GroupCacheDto {
        private Long id;
        private String groupName;
        private List<String> memberEmails;
        private Map<String, String> settings;

        public GroupCacheDto() {}

        public GroupCacheDto(Long id, String groupName, List<String> memberEmails, Map<String, String> settings) {
            this.id = id;
            this.groupName = groupName;
            this.memberEmails = memberEmails;
            this.settings = settings;
        }

        public Long getId() { return id; }
        public String getGroupName() { return groupName; }
        public List<String> getMemberEmails() { return memberEmails; }
        public Map<String, String> getSettings() { return settings; }
    }
}
