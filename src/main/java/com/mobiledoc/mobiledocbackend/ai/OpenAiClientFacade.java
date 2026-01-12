package com.mobiledoc.mobiledocbackend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;

import java.lang.reflect.Method;

public class OpenAiClientFacade {

    private static volatile OpenAIClient client;
    private static final ObjectMapper om = new ObjectMapper();

    private static OpenAIClient getClient() {
        if (client == null) {
            synchronized (OpenAiClientFacade.class) {
                if (client == null) {
                    client = OpenAIOkHttpClient.fromEnv();
                }
            }
        }
        return client;
    }

    // 기존 호환
    public static String callResponsesApi(String prompt) {
        return callResponsesApi(prompt, 350);
    }

    // ✅ 길이 조절용
    public static String callResponsesApi(String prompt, int maxOutputTokens) {
        ResponseCreateParams params = ResponseCreateParams.builder()
                .model("gpt-4o-mini")
                .input(prompt)
                .maxOutputTokens(maxOutputTokens)
                .build();

        Response response = getClient().responses().create(params);

        String viaHelper = tryCallOutputText(response);
        if (viaHelper != null && !viaHelper.isBlank()) return viaHelper;

        String viaJson = extractOutputTextFromJson(response);
        if (viaJson != null && !viaJson.isBlank()) return viaJson;

        return String.valueOf(response);
    }

    private static String tryCallOutputText(Response response) {
        try {
            Method m = response.getClass().getMethod("outputText");
            Object out = m.invoke(response);
            return out == null ? null : out.toString();
        } catch (Exception ignore) {
            return null;
        }
    }

    private static String extractOutputTextFromJson(Response response) {
        try {
            JsonNode root = om.valueToTree(response);

            JsonNode outputText = root.get("output_text");
            if (outputText != null && outputText.isTextual()) return outputText.asText();

            JsonNode output = root.get("output");
            if (output != null && output.isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode item : output) {
                    JsonNode content = item.get("content");
                    if (content != null && content.isArray()) {
                        for (JsonNode c : content) {
                            JsonNode text = c.get("text");
                            if (text != null && text.isTextual()) {
                                if (sb.length() > 0) sb.append("\n");
                                sb.append(text.asText());
                            }
                        }
                    }
                }
                if (sb.length() > 0) return sb.toString();
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
