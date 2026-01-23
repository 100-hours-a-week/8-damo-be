package com.team8.damo.service;

import com.team8.damo.exception.CustomException;
import com.team8.damo.exception.errorcode.ErrorCode;
import com.team8.damo.service.request.PresignedUrlServiceRequest;
import com.team8.damo.service.response.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Presigner s3Presigner;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    private static final int PRESIGNED_URL_EXPIRATION_MINUTES = 5;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/png",
        "image/jpeg",
        "image/jpg",
        "image/webp"
    );

    public PresignedUrlResponse generatePresignedUrl(PresignedUrlServiceRequest request) {
        validateContentType(request.contentType());
        String objectKey = generateObjectKey(request.directory(), request.fileName());

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(objectKey)
            .contentType(request.contentType())
            .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(PRESIGNED_URL_EXPIRATION_MINUTES))
            .putObjectRequest(putObjectRequest)
            .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        return new PresignedUrlResponse(
            presignedRequest.url().toString(),
            objectKey,
            PRESIGNED_URL_EXPIRATION_MINUTES * 60
        );
    }

    private String generateObjectKey(String directory, String fileName) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String sanitizedFileName = sanitizeFileName(fileName);

        if (directory != null && !directory.isBlank()) {
            return directory + "/" + timestamp + "_" + sanitizedFileName;
        }
        return timestamp + "_" + sanitizedFileName;
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9가-힣._-]", "_");
    }

    private void validateContentType(String contentType) {
        if (!ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }
    }
}
