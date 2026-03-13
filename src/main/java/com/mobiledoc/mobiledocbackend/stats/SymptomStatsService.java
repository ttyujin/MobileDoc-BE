// src/main/java/com/mobiledoc/mobiledocbackend/stats/SymptomStatsService.java
package com.mobiledoc.mobiledocbackend.stats;

import com.mobiledoc.mobiledocbackend.stats.dto.SymptomStatsResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class SymptomStatsService {

    private final SymptomLogRepository repo;

    public SymptomStatsService(SymptomLogRepository repo) {
        this.repo = repo;
    }

    public SymptomStatsResponse getSymptoms(String email, int days) {
        SymptomStatsResponse res = new SymptomStatsResponse();
        res.days = Math.max(days, 1);

        if (email == null || email.trim().isEmpty()) {
            res.totalCount = 0;
            return res;
        }

        LocalDateTime from = LocalDateTime.now().minusDays(res.days);
        List<SymptomLog> logs =
                repo.findByUserEmailAndCreatedAtAfterOrderByCreatedAtDesc(email.trim(), from);

        res.totalCount = logs.size();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        logs.stream().limit(5).forEach(l -> {
            SymptomStatsResponse.SymptomEntry e = new SymptomStatsResponse.SymptomEntry();
            e.date = (l.getCreatedAt() == null) ? "" : l.getCreatedAt().format(fmt);
            e.symptom = l.getSymptom();
            e.decisionLevel = l.getDecisionLevel();
            res.recent.add(e);
        });

        return res;
    }
}