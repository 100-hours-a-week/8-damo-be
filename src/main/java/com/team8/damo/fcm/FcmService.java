package com.team8.damo.fcm;

import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class FcmService {

    public String sendToToken(String token, String title, String body) {
        try {
            Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build())
                .setWebpushConfig(WebpushConfig.builder()
                    .setFcmOptions(WebpushFcmOptions.builder()
                        .setLink("https://www.naver.com")
                        .build())
                    .build())
                .build();

            String messageId = FirebaseMessaging.getInstance().send(message);

            MulticastMessage multicastMessage = MulticastMessage.builder()
                .addAllTokens(List.of("123", "124", "125"))
                .setNotification(Notification.builder().build())
                .build();

            log.info("FCM message sent successfully. messageId={}", messageId);
            return messageId;
        } catch (Exception e) {
            log.error("Failed to send FCM message to token={}", token, e);
            throw new RuntimeException("FCM send failed", e);
        }
    }

    public String sendWithData(String token, String title, String body, Map<String, String> data) {
        try {
            Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build())
                .putAllData(data)
                .build();

            String messageId = FirebaseMessaging.getInstance().send(message);
            log.info("FCM data message sent. messageId={}", messageId);
            return messageId;
        } catch (Exception e) {
            log.error("Failed to send FCM data message", e);
            throw new RuntimeException("FCM send failed", e);
        }
    }

    /**
     * 다건 FCM 발송 (최대 500건 배치).
     *
     * @return 발송 실패한 토큰 목록 (UNREGISTERED / INVALID_ARGUMENT)
     */
    public List<String> sendMulticast(List<String> fcmTokens, String title, String body, Map<String, String> data) {
        List<String> failedTokens = new ArrayList<>();

        for (int i = 0; i < fcmTokens.size(); i += 500) {
            List<String> batch = fcmTokens.subList(i, Math.min(i + 500, fcmTokens.size()));
            MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(batch)
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build())
                .putAllData(data)
                .build();

            try {
                BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
                List<SendResponse> responses = response.getResponses();

                for (int j = 0; j < responses.size(); j++) {
                    if (!responses.get(j).isSuccessful()) {
                        FirebaseMessagingException exception = responses.get(j).getException();
                        log.warn("[FcmService] Failed to send to token={}, error={}",
                            batch.get(j), exception != null ? exception.getMessage() : "unknown");

                        if (exception != null) {
                            MessagingErrorCode errorCode = exception.getMessagingErrorCode();
                            if (errorCode == MessagingErrorCode.UNREGISTERED
                                || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                                failedTokens.add(batch.get(j));
                            }
                        }
                    }
                }
            } catch (FirebaseMessagingException e) {
                log.error("[FcmService] sendMulticast batch failed", e);
            }
        }

        return failedTokens;
    }
}
