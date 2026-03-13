package com.mobiledoc.mobiledocbackend.alerts.dto;

public class ChecklistSummaryEmailRequest {
    public String toEmail;

    // 프론트가 어떤 구조로 보내도 받기 위해 Object로 받음(List/Map 모두 OK)
    public Object decision;   // 1분 판별 결과(있으면)
    public Object answers;    // 1분 질문 답변(있으면)
    public Object checklist;  // 3분 체크리스트(현재는 배열)
    public Object context;    // profileSummary 등(선택)
}