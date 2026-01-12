package com.mobiledoc.mobiledocbackend.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {
    Optional<EmailVerification> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email, VerificationPurpose purpose);
    void deleteByEmailAndPurpose(String email, VerificationPurpose purpose);
}
