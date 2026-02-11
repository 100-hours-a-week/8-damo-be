package com.team8.damo.entity;

import com.team8.damo.entity.enumeration.AgeGroup;
import com.team8.damo.entity.enumeration.Gender;
import com.team8.damo.entity.enumeration.OnboardingStep;
import com.team8.damo.entity.enumeration.RoleType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.STRING;

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
public class User extends BaseTimeEntity implements Persistable<Long> {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "nickname", length = 10)
    private String nickname;

    @Enumerated(STRING)
    @Column(name = "gender", length = 10)
    private Gender gender;

    @Enumerated(STRING)
    @Column(name = "age_group", length = 20)
    private AgeGroup ageGroup;

    @Column(name = "is_push_notification_allowed", nullable = false)
    private boolean isPushNotificationAllowed = false;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Enumerated(STRING)
    @Column(name = "onboarding_step", nullable = false, length = 20)
    private OnboardingStep onboardingStep = OnboardingStep.BASIC;

    @Column(name = "fcm_token", length = 255)
    private String fcmToken;

    @Column(name = "image_path", length = 200)
    private String imagePath;

    @Column(name = "withdraw_at")
    private LocalDateTime withdrawAt;

    @Column(name = "is_withdraw", nullable = false)
    private boolean isWithdraw = false;

    @Column(name = "other_characteristics", length = 100)
    private String otherCharacteristics;

    @Enumerated(STRING)
    @Column(name = "role_type", length = 20)
    private RoleType roleType = RoleType.ROLE_USER;

    public User(Long id, String email, Long providerId) {
        this.id = id;
        this.email = email;
        this.providerId = providerId;
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

    public void changeImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public void enablePushNotification(String fcmToken) {
        this.isPushNotificationAllowed = true;
        this.fcmToken = fcmToken;
    }

    public void disablePushNotification() {
        this.isPushNotificationAllowed = false;
        this.fcmToken = null;
    }

    @Override
    public boolean isNew() {
        return getCreatedAt() == null;
    }

    public void withdraw() {
        this.isWithdraw = true;
        this.withdrawAt = LocalDateTime.now();
    }

    public void updateBasic(String nickname, Gender gender, AgeGroup ageGroup) {
        this.nickname = nickname;
        this.gender = gender;
        this.ageGroup = ageGroup;
        if (this.onboardingStep != OnboardingStep.DONE) {
            this.onboardingStep = OnboardingStep.CHARACTERISTIC;
        }
    }
}
