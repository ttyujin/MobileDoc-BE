package com.mobiledoc.mobiledocbackend.profile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static lombok.AccessLevel.PROTECTED;

@Getter
@NoArgsConstructor(access = PROTECTED)
@Entity
@Table(name = "profile_visit_history")
public class ProfileVisitHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    private LocalDate date;
    private String hospital;
    private String diagnosis;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public ProfileVisitHistory(UUID userId, LocalDate date, String hospital, String diagnosis) {
        this.userId = userId;
        this.date = date;
        this.hospital = hospital;
        this.diagnosis = diagnosis;
        this.createdAt = Instant.now();
    }
}
