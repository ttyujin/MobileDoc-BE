package com.mobiledoc.mobiledocbackend.ai.dto;

import java.util.List;
import java.util.Map;

public class ChatRequest {
    public String category;

    // 프론트 호환: text/message 둘 다 받을 수 있게
    public String text;
    public String message;

    // 최근 대화(없을 수도 있음)
    public List<ChatMessage> history;

    // profileSummary, lastDecision 등(없을 수도 있음)
    public Map<String, Object> context;

    public String userId;

    // 프론트가 주면 사용, 없으면 서버 고정값 사용
    public String customerCenterPhone;

    public String getUserText() {
        if (text != null && !text.isBlank()) return text;
        if (message != null && !message.isBlank()) return message;
        return "";
    }
}