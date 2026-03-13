package com.mobiledoc.mobiledocbackend.ai.dto;

public class ChatMessage {
    public String role;    // "user" | "assistant" | "system"
    public String content;

    public ChatMessage() {}

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
}