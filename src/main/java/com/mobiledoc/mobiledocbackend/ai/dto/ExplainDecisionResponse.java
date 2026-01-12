package com.mobiledoc.mobiledocbackend.ai.dto;

import java.util.List;

public class ExplainDecisionResponse {
    public String summary;   // 짧은 요약
    public String detail;    // ✅ 긴 설명(왜 그런지)
    public List<String> bullets;
    public List<String> ask;

    public ExplainDecisionResponse() {}

    public ExplainDecisionResponse(String summary, String detail, List<String> bullets, List<String> ask) {
        this.summary = summary;
        this.detail = detail;
        this.bullets = bullets;
        this.ask = ask;
    }
}
