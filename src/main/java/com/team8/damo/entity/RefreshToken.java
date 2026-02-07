package com.team8.damo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    private String email;

    private String token;

    public RefreshToken(String email, String token) {
        this.email = email;
        this.token = token;
    }

    public boolean isNotSameToken(String token) {
        return !this.token.equals(token);
    }
}
