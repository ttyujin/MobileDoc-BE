// src/main/java/com/mobiledoc/mobiledocbackend/stats/dto/SymptomStatsResponse.java
package com.mobiledoc.mobiledocbackend.stats.dto;

import java.util.ArrayList;
import java.util.List;

public class SymptomStatsResponse {
    public int days;
    public int totalCount;
    public List<SymptomEntry> recent = new ArrayList<>();

    public static class SymptomEntry {
        public String date;          // yyyy-MM-dd
        public String symptom;       // cold/skin/...
        public String decisionLevel; // ok/conditional/...
    }
}