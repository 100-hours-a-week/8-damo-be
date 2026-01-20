package com.team8.damo.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_USERS_EMAIL", columnNames = "email"),
        @UniqueConstraint(name = "UK_USERS_NICKNAME", columnNames = "nickname")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "nickname", nullable = false, length = 10)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 10)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_group", nullable = false, length = 20)
    private AgeGroup ageGroup;

    @Column(name = "is_push_notification_allowed", nullable = false)
    private boolean isPushNotificationAllowed = false;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_step", nullable = false, length = 20)
    private OnboardingStep onboardingStep = OnboardingStep.BASIC;

    @Column(name = "fcm_token", length = 255)
    private String fcmToken;

    @Column(name = "withdraw_at")
    private LocalDateTime withdrawAt;

    @Column(name = "is_withdraw", nullable = false)
    private boolean isWithdraw = false;

    @Column(name = "other_characteristics", nullable = true, length = 100)
    private String otherCharacteristics;

    @Builder
    public User(Long id, String email, String password, String nickname, Gender gender,
                AgeGroup ageGroup, Long providerId, String otherCharacteristics) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.gender = gender;
        this.ageGroup = ageGroup;
        this.providerId = providerId;
        this.otherCharacteristics = otherCharacteristics;
        this.isPushNotificationAllowed = false;
        this.isWithdraw = false;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateOnboardingStep(OnboardingStep step) {
        this.onboardingStep = step;
    }

    public void updateOtherCharacteristics(String characteristics) {
        this.otherCharacteristics = characteristics;
    }

    public void enablePushNotification(String fcmToken) {
        this.isPushNotificationAllowed = true;
        this.fcmToken = fcmToken;
    }

    public void disablePushNotification() {
        this.isPushNotificationAllowed = false;
        this.fcmToken = null;
    }

    public void withdraw() {
        this.isWithdraw = true;
        this.withdrawAt = LocalDateTime.now();
    }
}
