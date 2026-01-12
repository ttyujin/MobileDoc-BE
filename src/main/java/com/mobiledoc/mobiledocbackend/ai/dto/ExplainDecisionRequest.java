package com.mobiledoc.mobiledocbackend.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExplainDecisionRequest {
    public String decisionLevel;          // ok / conditional / inperson
    public Map<String, Object> answers;   // 프론트에서 온 answers

    // ✅ (선택) 룰 기반 근거를 같이 보내면 “왜?”가 훨씬 좋아짐
    public String title;
    public String oneLineReason;
    public List<String> reasons;

    // ✅ 1(간단) / 2(보통) / 3(자세히)
    public Integer detailLevel;
}
