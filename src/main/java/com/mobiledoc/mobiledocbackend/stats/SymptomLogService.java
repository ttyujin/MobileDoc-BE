package com.mobiledoc.mobiledocbackend.stats;

import com.mobiledoc.mobiledocbackend.alerts.dto.ChecklistSummaryEmailRequest;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SymptomLogService {

    private final SymptomLogRepository repo;

    public SymptomLogService(SymptomLogRepository repo) {
        this.repo = repo;
    }

    public void recordFromChecklist(ChecklistSummaryEmailRequest req) {
        if (req == null) return;

        String email = safe(req.toEmail);
        if (email.isEmpty()) return;

        Map<?, ?> answers = toMap(req.answers);
        Map<?, ?> decision = toMap(req.decision);

        String symptom = safe(answers.get("symptom"));
        if (symptom.isEmpty()) return;

        String severity = safe(answers.get("severity"));
        String level = safe(decision.get("level"));

        SymptomLog log = new SymptomLog();
        log.setUserEmail(email);
        log.setSymptom(symptom);
        log.setSeverity(severity.isEmpty() ? null : severity);
        log.setDecisionLevel(level.isEmpty() ? null : level);

        repo.save(log);
    }

    private Map<?, ?> toMap(Object obj) {
        if (obj instanceof Map<?, ?> m) return m;
        return Map.of();
    }

    private String safe(Object v) {
        return v == null ? "" : v.toString().trim();
    }
}