package com.mobiledoc.mobiledocbackend.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

import static lombok.AccessLevel.PROTECTED;

@Getter
@NoArgsConstructor(access = PROTECTED)
@Entity
@Table(
        name = "email_verifications",
        indexes = { @Index(name = "idx_ev_email_purpose", columnList = "email,purpose") }
)
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String email; // lowercased

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VerificationPurpose purpose;

    @Column(nullable = false)
    private String codeHash; // BCrypt

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant verifiedAt;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant lastSentAt = Instant.now();

    public EmailVerification(String email, VerificationPurpose purpose, String codeHash, Instant expiresAt) {
        this.email = email;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
    }

    public void markSentNow() {
        this.lastSentAt = Instant.now();
    }

    public void incAttempts() {
        this.attempts += 1;
    }

    public void markVerified() {
        this.verifiedAt = Instant.now();
    }
}
