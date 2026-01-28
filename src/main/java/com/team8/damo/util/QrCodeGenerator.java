package com.team8.damo.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class QrCodeGenerator {

    private final String redirectUrl;
    private final String imageFormat;

    private final S3Client s3Client;
    private final String bucket;

    public QrCodeGenerator(
        S3Client s3Client,
        @Value("${qr.redirect-url}") String redirectUrl,
        @Value("${qr.image-format}") String imageFormat,
        @Value("${spring.cloud.aws.s3.bucket}") String bucket
    ) {
        this.s3Client = s3Client;
        this.redirectUrl = redirectUrl;
        this.imageFormat = imageFormat;
        this.bucket = bucket;
    }

    public void generateQrCode(Long groupId) {
        try {
            String objectKey = "groups/qr/" + groupId;

            BitMatrix bitMatrix = new QRCodeWriter()
                .encode(redirectUrl + groupId, BarcodeFormat.QR_CODE, 100, 100);

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                MatrixToImageWriter.writeToStream(bitMatrix, imageFormat, outputStream);

                PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType("image/png")
                    .build();

                s3Client.putObject(request, RequestBody.fromBytes(outputStream.toByteArray()));
            }
        } catch (IOException | WriterException e) {
            throw new RuntimeException(e);
        }
    }
}
