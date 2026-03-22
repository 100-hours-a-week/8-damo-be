package com.team8.damo.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Slf4j
@Configuration
public class FcmConfig {

    @Value("${fcm.service-account-file}")
    private String serviceAccountFile;

    @PostConstruct
    public void init() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                GoogleCredentials credentials = GoogleCredentials
                        .fromStream(new ClassPathResource(serviceAccountFile).getInputStream());

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("FirebaseApp initialized successfully");
            }
        } catch (IOException e) {
            log.error("Failed to initialize FirebaseApp", e);
            throw new RuntimeException("Firebase initialization failed", e);
        }
    }
}
