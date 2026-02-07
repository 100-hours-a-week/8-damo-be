package com.team8.damo.service;

import com.team8.damo.exception.CustomException;
import com.team8.damo.service.request.PresignedUrlServiceRequest;
import com.team8.damo.service.response.PresignedUrlResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;

import static com.team8.damo.exception.errorcode.ErrorCode.INVALID_FILE_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Presigner s3Presigner;

    @InjectMocks
    private S3Service s3Service;

    private static final String DIRECTORY_PREFIX = "s3/images";
    private static final String UUID_REGEX = "(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    @Test
    @DisplayName("Presigned URL을 성공적으로 발급한다.")
    void generatePresignedUrl_success() throws Exception {
        // given
        ReflectionTestUtils.setField(s3Service, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(s3Service, "directoryPrefix", DIRECTORY_PREFIX);

        String fileName = "profile.jpg";
        String contentType = "image/jpeg";
        String directory = "user/profile";

        PresignedUrlServiceRequest request = new PresignedUrlServiceRequest(
            fileName, contentType, directory
        );

        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        given(presignedRequest.url()).willReturn(new URL("https://test-bucket.s3.amazonaws.com/test-key"));
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presignedRequest);

        // when
        PresignedUrlResponse response = s3Service.generatePresignedUrl(request);

        // then
        assertThat(response)
            .extracting("presignedUrl", "expiresIn")
            .containsExactly("https://test-bucket.s3.amazonaws.com/test-key", 300);
        assertObjectKeyWithPrefix(response.objectKey(), DIRECTORY_PREFIX + "/" + directory + "/");

        then(s3Presigner).should().presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    @DisplayName("디렉토리 없이 Presigned URL을 발급한다.")
    void generatePresignedUrl_withoutDirectory() throws Exception {
        // given
        ReflectionTestUtils.setField(s3Service, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(s3Service, "directoryPrefix", DIRECTORY_PREFIX);

        String fileName = "image.png";
        String contentType = "image/png";

        PresignedUrlServiceRequest request = new PresignedUrlServiceRequest(
            fileName, contentType, null
        );

        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        given(presignedRequest.url()).willReturn(new URL("https://test-bucket.s3.amazonaws.com/test-key"));
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presignedRequest);

        // when
        PresignedUrlResponse response = s3Service.generatePresignedUrl(request);

        // then
        assertObjectKeyWithPrefix(response.objectKey(), DIRECTORY_PREFIX + "/");

        then(s3Presigner).should().presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    @DisplayName("빈 디렉토리로 Presigned URL을 발급하면 디렉토리 없이 생성된다.")
    void generatePresignedUrl_emptyDirectory() throws Exception {
        // given
        ReflectionTestUtils.setField(s3Service, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(s3Service, "directoryPrefix", DIRECTORY_PREFIX);

        String fileName = "image.webp";
        String contentType = "image/webp";

        PresignedUrlServiceRequest request = new PresignedUrlServiceRequest(
            fileName, contentType, "   "
        );

        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        given(presignedRequest.url()).willReturn(new URL("https://test-bucket.s3.amazonaws.com/test-key"));
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presignedRequest);

        // when
        PresignedUrlResponse response = s3Service.generatePresignedUrl(request);

        // then
        assertObjectKeyWithPrefix(response.objectKey(), DIRECTORY_PREFIX + "/");

        then(s3Presigner).should().presignPutObject(any(PutObjectPresignRequest.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/png", "image/jpeg", "image/jpg", "image/webp"})
    @DisplayName("허용된 Content-Type으로 Presigned URL을 발급한다.")
    void generatePresignedUrl_allowedContentTypes(String contentType) throws Exception {
        // given
        ReflectionTestUtils.setField(s3Service, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(s3Service, "directoryPrefix", DIRECTORY_PREFIX);

        PresignedUrlServiceRequest request = new PresignedUrlServiceRequest(
            "test.file", contentType, null
        );

        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        given(presignedRequest.url()).willReturn(new URL("https://test-bucket.s3.amazonaws.com/test-key"));
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presignedRequest);

        // when
        PresignedUrlResponse response = s3Service.generatePresignedUrl(request);

        // then
        assertThat(response)
            .extracting("presignedUrl", "expiresIn")
            .containsExactly("https://test-bucket.s3.amazonaws.com/test-key", 300);

        then(s3Presigner).should().presignPutObject(any(PutObjectPresignRequest.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"IMAGE/PNG", "IMAGE/JPEG", "Image/Webp"})
    @DisplayName("대소문자 구분 없이 허용된 Content-Type으로 Presigned URL을 발급한다.")
    void generatePresignedUrl_caseInsensitiveContentTypes(String contentType) throws Exception {
        // given
        ReflectionTestUtils.setField(s3Service, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(s3Service, "directoryPrefix", DIRECTORY_PREFIX);

        PresignedUrlServiceRequest request = new PresignedUrlServiceRequest(
            "test.file", contentType, null
        );

        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        given(presignedRequest.url()).willReturn(new URL("https://test-bucket.s3.amazonaws.com/test-key"));
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presignedRequest);

        // when
        PresignedUrlResponse response = s3Service.generatePresignedUrl(request);

        // then
        assertThat(response)
            .extracting("presignedUrl", "expiresIn")
            .containsExactly("https://test-bucket.s3.amazonaws.com/test-key", 300);

        then(s3Presigner).should().presignPutObject(any(PutObjectPresignRequest.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/gif", "image/bmp", "application/pdf", "text/plain", "video/mp4"})
    @DisplayName("허용되지 않은 Content-Type으로 요청하면 예외가 발생한다.")
    void generatePresignedUrl_invalidContentType(String contentType) {
        // given
        PresignedUrlServiceRequest request = new PresignedUrlServiceRequest(
            "test.file", contentType, null
        );

        // when // then
        assertThatThrownBy(() -> s3Service.generatePresignedUrl(request))
            .isInstanceOf(CustomException.class)
            .hasFieldOrPropertyWithValue("errorCode", INVALID_FILE_TYPE);

        then(s3Presigner).should(never()).presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    @DisplayName("파일명에 특수문자가 포함되면 sanitize 처리된다.")
    void generatePresignedUrl_sanitizeFileName() throws Exception {
        // given
        ReflectionTestUtils.setField(s3Service, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(s3Service, "directoryPrefix", DIRECTORY_PREFIX);

        String fileName = "test file@#$.jpg";
        String contentType = "image/jpeg";

        PresignedUrlServiceRequest request = new PresignedUrlServiceRequest(
            fileName, contentType, "profiles"
        );

        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        given(presignedRequest.url()).willReturn(new URL("https://test-bucket.s3.amazonaws.com/test-key"));
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presignedRequest);

        // when
        PresignedUrlResponse response = s3Service.generatePresignedUrl(request);

        // then
        assertThat(response.objectKey())
            .startsWith(DIRECTORY_PREFIX + "/profiles/")
            .doesNotContain("@", "#", "$", " ");

        then(s3Presigner).should().presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    @DisplayName("한글 파일명은 유지된다.")
    void generatePresignedUrl_koreanFileName() throws Exception {
        // given
        ReflectionTestUtils.setField(s3Service, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(s3Service, "directoryPrefix", DIRECTORY_PREFIX);

        String fileName = "프로필사진.png";
        String contentType = "image/png";

        PresignedUrlServiceRequest request = new PresignedUrlServiceRequest(
            fileName, contentType, null
        );

        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        given(presignedRequest.url()).willReturn(new URL("https://test-bucket.s3.amazonaws.com/test-key"));
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presignedRequest);

        // when
        PresignedUrlResponse response = s3Service.generatePresignedUrl(request);

        // then
        assertThat(response)
            .extracting("objectKey")
            .asString()
            .startsWith(DIRECTORY_PREFIX + "/");

        then(s3Presigner).should().presignPutObject(any(PutObjectPresignRequest.class));
    }

    private void assertObjectKeyWithPrefix(String objectKey, String prefix) {
        assertThat(objectKey).startsWith(prefix);
        String suffix = objectKey.substring(prefix.length());
        assertThat(suffix).matches(UUID_REGEX);
    }
}
