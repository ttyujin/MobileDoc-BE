// src/main/java/com/mobiledoc/mobiledocbackend/stats/SymptomStatsController.java
package com.mobiledoc.mobiledocbackend.stats;

import com.mobiledoc.mobiledocbackend.stats.dto.SymptomStatsResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stats")
public class SymptomStatsController {

    private final SymptomStatsService symptomStatsService;

    public SymptomStatsController(SymptomStatsService symptomStatsService) {
        this.symptomStatsService = symptomStatsService;
    }

    @GetMapping("/symptoms")
    public SymptomStatsResponse symptoms(
            @RequestParam(required = false) String email,
            @RequestParam(required = false, defaultValue = "30") int days
    ) {
        return symptomStatsService.getSymptoms(email, days);
    }
}