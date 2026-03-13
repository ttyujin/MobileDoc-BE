// src/main/java/com/mobiledoc/mobiledocbackend/stats/SymptomLog.java
package com.mobiledoc.mobiledocbackend.stats;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "symptom_logs")
public class SymptomLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 최소 구현: 이메일 기준으로 유저 구분
    @Column(nullable = false, length = 190)
    private String userEmail;

    // answers.symptom (예: cold/skin/gi...)
    @Column(nullable = false, length = 190)
    private String symptom;

    // answers.severity (있으면)
    @Column(length = 190)
    private String severity;

    // decision.level (ok/conditional/inperson/emergency)
    @Column(length = 50)
    private String decisionLevel;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getSymptom() { return symptom; }
    public void setSymptom(String symptom) { this.symptom = symptom; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getDecisionLevel() { return decisionLevel; }
    public void setDecisionLevel(String decisionLevel) { this.decisionLevel = decisionLevel; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}