package com.mobiledoc.mobiledocbackend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobiledoc.mobiledocbackend.ai.dto.ChatMessage;
import com.mobiledoc.mobiledocbackend.ai.dto.ChatRequest;
import com.mobiledoc.mobiledocbackend.ai.dto.ChatResponse;
import com.mobiledoc.mobiledocbackend.ai.dto.ExplainDecisionRequest;
import com.mobiledoc.mobiledocbackend.ai.dto.ExplainDecisionResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiExplainService {

    private final ObjectMapper om = new ObjectMapper();

    // ✅ 고객센터 번호 고정 (프론트 정책)
    private static final String DEFAULT_CC_PHONE = "010-4227-5689";

    private boolean hasOpenAiKey() {
        String key = System.getenv("OPENAI_API_KEY");
        return key != null && !key.isBlank();
    }

    // =========================
    // ✅ 챗봇: /ai/chat
    // =========================
    public ChatResponse chat(ChatRequest req) {
        String category = normalizeCategory(req != null ? req.category : null);
        String userText = (req != null) ? req.getUserText() : "";
        Map<String, Object> context = (req != null) ? req.context : null;
        List<ChatMessage> history = (req != null) ? req.history : null;

        String phone = DEFAULT_CC_PHONE;
        if (req != null && req.customerCenterPhone != null && !req.customerCenterPhone.isBlank()) {
            phone = req.customerCenterPhone.trim();
        }

        if (userText == null) userText = "";
        userText = userText.trim();

        // ✅ 키 없으면 폴백(프론트 에러 없이 동작)
        if (!hasOpenAiKey()) {
            String fb = chatFallback(category, userText, context, phone);
            fb = enforceContactPhone(category, fb, phone);
            return new ChatResponse(fb);
        }

        try {
            String prompt = buildChatPrompt(category, userText, context, history, phone);

            // 챗봇은 길게 나올 수 있으니 넉넉히
            int maxTokens = 900;

            String reply = OpenAiClientFacade.callResponsesApi(prompt, maxTokens);
            if (reply == null || reply.isBlank()) {
                String fb = chatFallback(category, userText, context, phone);
                fb = enforceContactPhone(category, fb, phone);
                return new ChatResponse(fb);
            }

            reply = postProcessSafety(reply, userText);
            reply = enforceContactPhone(category, reply, phone);

            return new ChatResponse(reply.trim());
        } catch (Exception e) {
            String fb = chatFallback(category, userText, context, phone);
            fb = enforceContactPhone(category, fb, phone);
            return new ChatResponse(fb);
        }
    }

    private String normalizeCategory(String c) {
        if (c == null) return "decision";
        String x = c.trim().toLowerCase();
        return switch (x) {
            case "visits", "symptoms", "decision", "contact" -> x;
            default -> "decision";
        };
    }

    private String buildChatPrompt(
            String category,
            String userText,
            Map<String, Object> context,
            List<ChatMessage> history,
            String customerCenterPhone
    ) {
        String policy = """
                너는 MobileDoc 앱의 챗봇이다. 한국어로 답한다.

                절대 규칙(중요):
                - 의료 진단/처방/약 추천을 하지 마라. (절차/안전/정책 안내만)
                - 사용자가 응급으로 보이면 '119/응급실' 우선 안내를 반드시 포함해라.
                - 아래 context에 있는 정보만 근거로 말해라. context에 없으면 "현재 기록이 없어서 단정할 수 없다"라고 말해라. (날조 금지)
                - category가 contact이면 답변에 고객센터 번호 %s 를 반드시 포함해라.
                """.formatted(customerCenterPhone);

        String categoryRule = switch (category) {
            case "visits" -> """
                    category=visits (방문병원)
                    - context에 방문 기록/자주 간 병원 정보가 있으면 그것을 요약해라.
                    - 없으면 기록이 없다고 말하고, 어떤 정보를 입력/저장하면 정리가 가능한지 안내해라.
                    - 형식: 요약 1~2문장 + 목록(있으면) + 다음 행동 1~2개.
                    """;
            case "symptoms" -> """
                    category=symptoms (증상통계)
                    - context에 증상/진료과/방문 통계가 있으면 '횟수/패턴' 중심으로 요약해라.
                    - 통계가 없으면 통계 데이터가 없다고 말하고, 무엇이 기록돼야 통계가 되는지 안내해라.
                    - 질병 추정/진단은 금지.
                    """;
            case "decision" -> """
                    category=decision (최근판별)
                    - context에 lastDecision / reasons / summary가 있으면, 쉬운 말로 풀어 설명해라.
                    - 없으면 최근 판별 기록이 없다고 말하고, 어떤 정보가 필요할지 안내해라.
                    - 안전 안내(악화/응급 시 대면/응급)도 덧붙여라.
                    """;
            case "contact" -> """
                    category=contact (고객센터)
                    - 1) 확인 질문 2) 간단 해결 체크리스트 3) 고객센터 안내 순서로 답해라.
                    - 반드시 고객센터 번호를 포함해라.
                    """;
            default -> "category=decision";
        };

        String historyText = safeHistory(history);
        String ctx = safeToJson(context);

        return """
                %s

                %s

                (대화 히스토리 - 있으면 참고)
                %s

                (context - 이 정보만 근거로 사용)
                %s

                (사용자 질문)
                %s

                답변 스타일:
                - 짧은 문장 위주, 어려운 용어 금지
                - 필요한 경우에만 항목/번호로 정리
                - 길이는 6~14문장 정도(카테고리에 맞게)
                """.formatted(policy, categoryRule, historyText, ctx, safe(userText));
    }

    private String safeHistory(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) return "(없음)";
        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, history.size() - 10);
        for (int i = start; i < history.size(); i++) {
            ChatMessage m = history.get(i);
            if (m == null) continue;
            String role = (m.role == null) ? "unknown" : m.role.trim();
            String content = (m.content == null) ? "" : m.content.trim();
            if (content.isBlank()) continue;
            sb.append("- ").append(role).append(": ").append(content.replace("\n", " ")).append("\n");
        }
        String out = sb.toString().trim();
        return out.isBlank() ? "(없음)" : out;
    }

    private String chatFallback(String category, String userText, Map<String, Object> context, String phone) {
        boolean emergency = looksEmergency(userText);

        String emergencyLine = emergency
                ? "지금 증상이 심하거나 급격히 악화 중이면, 먼저 119/응급실을 우선으로 고려해 주세요.\n\n"
                : "";

        return switch (category) {
            case "visits" -> emergencyLine + """
                    방문병원 정리는 “저장된 기록”이 있을 때 정확해요.
                    지금은 서버에 넘어온 방문 기록(context)이 없어서 단정해서 정리하긴 어려워요.

                    가능하면 아래 정보를 알려주면 바로 정리해줄 수 있어요:
                    - 병원 이름(또는 자주 간 병원)
                    - 최근 방문 날짜/목적(간단히)
                    """;
            case "symptoms" -> emergencyLine + """
                    증상 통계는 기록이 있어야 정확히 계산돼요.
                    지금은 서버에 넘어온 통계(context)가 없어서 “몇 번” 같은 숫자를 확정할 수 없어요.

                    통계를 원하면:
                    - 증상/진료과 선택 기록
                    - 방문/상담 기록
                    이 두 가지가 쌓여야 해요.
                    """;
            case "decision" -> emergencyLine + """
                    최근 판별 결과를 설명하려면 lastDecision / reasons 같은 기록이 필요해요.
                    지금은 서버에 넘어온 판별 정보(context)가 없어서 “왜 조건부인지”를 정확히 특정할 수 없어요.

                    만약 결과 화면에서 본 문구(요약/근거)를 그대로 붙여주면,
                    그 근거를 바탕으로 쉽게 풀어서 설명해줄게요.
                    """;
            case "contact" -> emergencyLine + """
                    로그인/계정 문제가 있을 때는 아래부터 빠르게 확인해 주세요.
                    1) 비밀번호 재설정(메일/인증) 진행 여부
                    2) 대소문자/공백/자동완성으로 이메일이 달라지지 않았는지
                    3) 다른 네트워크(와이파이/데이터)에서도 동일한지

                    그래도 해결이 안 되면 고객센터로 문의해 주세요: %s
                    """.formatted(phone);
            default -> emergencyLine + "지금은 해당 요청을 처리할 정보가 부족해요. 질문을 조금만 더 자세히 알려주세요.";
        };
    }

    private String enforceContactPhone(String category, String reply, String phone) {
        if (!"contact".equals(category)) return reply;
        if (reply == null) reply = "";
        if (reply.contains(phone)) return reply;
        return reply.trim() + "\n\n고객센터: " + phone;
    }

    private String postProcessSafety(String reply, String userText) {
        if (reply == null) return "";
        String out = reply.trim();

        // 응급으로 보이면 119 안내를 보강
        if (looksEmergency(userText) && !out.contains("119") && !out.contains("응급실")) {
            out = "지금 증상이 심하거나 급격히 악화 중이면, 먼저 119/응급실을 우선으로 고려해 주세요.\n\n" + out;
        }
        return out;
    }

    private boolean looksEmergency(String text) {
        if (text == null) return false;
        String t = text.toLowerCase();
        String[] keys = new String[] {
                "호흡", "숨", "가슴통증", "흉통", "의식", "실신", "마비", "경련",
                "피가", "출혈", "극심", "응급", "119", "심한 통증", "숨쉬기",
                "자살", "자해"
        };
        for (String k : keys) {
            if (t.contains(k.toLowerCase())) return true;
        }
        return false;
    }

    // =========================
    // ✅ 기존 explain-decision (너가 준 코드 그대로)
    // =========================
    public ExplainDecisionResponse explain(ExplainDecisionRequest req) {
        String level = (req != null && req.decisionLevel != null) ? req.decisionLevel : "conditional";
        Map<String, Object> answers = (req != null) ? req.answers : null;

        // ✅ 길이 기본값: 자세히(3)
        int detailLevel = (req != null && req.detailLevel != null) ? req.detailLevel : 3;
        if (detailLevel < 1) detailLevel = 1;
        if (detailLevel > 3) detailLevel = 3;

        String title = (req != null && req.title != null) ? req.title : "";
        String oneLineReason = (req != null && req.oneLineReason != null) ? req.oneLineReason : "";
        List<String> ruleReasons = (req != null && req.reasons != null) ? req.reasons : List.of();

        if (!hasOpenAiKey()) {
            return fallback(level, detailLevel);
        }

        try {
            String prompt = buildPrompt(level, detailLevel, title, oneLineReason, ruleReasons, answers);

            // ✅ detailLevel에 따라 토큰 상한을 늘림(길게 나오게)
            int maxTokens = switch (detailLevel) {
                case 1 -> 450;
                case 2 -> 850;
                default -> 1200;
            };

            String text = OpenAiClientFacade.callResponsesApi(prompt, maxTokens);

            ExplainDecisionResponse parsed = parseJson(text);
            if (parsed == null || parsed.summary == null || parsed.summary.isBlank()) {
                return fallback(level, detailLevel);
            }

            if (parsed.detail == null) parsed.detail = "";
            if (parsed.bullets == null) parsed.bullets = List.of();
            if (parsed.ask == null) parsed.ask = new ArrayList<>();
            while (parsed.ask.size() < 3) parsed.ask.add("오늘 비대면 진료 가능 여부를 확인해 주세요.");
            if (parsed.ask.size() > 3) parsed.ask = parsed.ask.subList(0, 3);

            return parsed;
        } catch (Exception e) {
            return fallback(level, detailLevel);
        }
    }

    private ExplainDecisionResponse fallback(String level, int detailLevel) {
        // fallback도 “detail” 포함(프론트가 긴 설명 영역을 렌더링하니까)
        if ("ok".equals(level)) {
            return new ExplainDecisionResponse(
                    "재진/비응급 조건이 있어 비대면 진행이 비교적 수월할 가능성이 높아요.",
                    detailLevel >= 2
                            ? "지금 선택하신 답변은 ‘응급 신호 없음’ + ‘비대면 가능’ 쪽으로 기울어 있어요.\n"
                            + "또 재진/결과상담/재처방 같은 케이스는 병원 입장에서 판단 근거(이전 기록)가 있어 진행이 쉬운 편이에요.\n"
                            + "다만 병원마다 정책이 달라서, 화상/전화 중 어떤 방식이 필요한지와 처방 가능 여부는 확인이 필요해요."
                            : "",
                    List.of("병원 기록이 있으면 승인/처방 판단이 쉬워요.", "화상이 가능하면 통과 확률이 더 올라가요."),
                    List.of("오늘 비대면 접수 가능한가요?", "전화/화상 중 어떤 방식이 필수인가요?", "처방이 가능하다면 수령 방법은 어떤가요?")
            );
        }

        if ("inperson".equals(level)) {
            return new ExplainDecisionResponse(
                    "현재 조건에서는 대면 진료가 더 안전하고 확실해요.",
                    detailLevel >= 2
                            ? "지금 답변 흐름은 ‘비대면으로 시작했다가 대면으로 전환될 가능성’이 큰 쪽이에요.\n"
                            + "증상이 심하거나(일상 불가 수준), 검사가 필요해 보이면 비대면은 한계가 있어요.\n"
                            + "처음부터 대면으로 가면 시간 낭비를 줄이고 필요한 검사/처치를 바로 받을 수 있어요."
                            : "",
                    List.of("검사/처치가 필요할 수 있어요.", "비대면은 관찰/검사가 제한돼요."),
                    List.of("지금 바로 방문 가능한 병원이 있나요?", "증상 악화 속도가 빠른가요?", "필요한 검사 가능 여부를 확인해 주세요.")
            );
        }

        return new ExplainDecisionResponse(
                "진행은 가능하지만 병원 정책/초진 제한/처방 조건 때문에 중간에 막힐 수 있어요.",
                detailLevel >= 2
                        ? "현재 답변 조합은 ‘가능’ 쪽이긴 하지만, 병원 규정에 따라 걸리는 지점이 있을 수 있어요.\n"
                        + "특히 새 증상(초진 가능성), 처방 필요, 수령 방식(배송/대리) 같은 요소는 병원마다 다르게 운영돼요.\n"
                        + "그래서 시작 전에 몇 가지를 확인하면 실패 확률을 크게 줄일 수 있어요."
                        : "",
                List.of("초진/새 증상/처방은 병원마다 규정이 달라요.", "전화만 가능하면 제한이 생길 수 있어요."),
                List.of("오늘 비대면 진료 가능한가요?", "초진(새 증상)도 가능한가요?", "처방/수령(배송/대리) 조건이 가능한가요?")
        );
    }

    private String buildPrompt(
            String level,
            int detailLevel,
            String title,
            String oneLineReason,
            List<String> ruleReasons,
            Map<String, Object> answers
    ) {
        // detailLevel별 최소 분량 지시
        String lengthRule = switch (detailLevel) {
            case 1 -> "detail은 3~5문장으로 짧게.";
            case 2 -> "detail은 7~10문장, 2단락 이상.";
            default -> "detail은 10~14문장, 최소 3단락(줄바꿈 포함).";
        };

        return """
                너는 MobileDoc의 '결과 설명' 도우미다.
                의료 진단/처방을 하지 말고, 사용자가 선택한 답변을 바탕으로
                안전/절차/정책 관점에서 '왜 이런 결론인지'를 쉽게 풀어 설명해라.

                중요한 규칙:
                - 아래의 ruleReasons(룰 기반 근거)를 반드시 기반으로 설명을 확장해라. 근거를 임의로 만들어내지 마라.
                - 응급 신호가 없더라도, 증상 심각도/악화 여부가 크면 '대면 권장'이 나올 수 있음을 안전하게 설명해라.
                - 사용자가 이해하기 쉬운 말(중학생 수준)로 써라.
                - 반드시 "오직 JSON"만 출력. 다른 텍스트/코드블록 금지.

                출력 JSON 스키마(키 이름 고정):
                {
                  "summary": "1~2문장(짧은 요약)",
                  "detail": "왜 이런 결론인지 자세한 설명(줄바꿈 포함 가능). %s",
                  "bullets": ["근거 1", "근거 2", "근거 3", "근거 4(선택)", "근거 5(선택)"],
                  "ask": ["병원에 확인할 질문 1", "질문 2", "질문 3"]
                }

                decisionLevel: %s
                title: %s
                oneLineReason: %s
                ruleReasons: %s
                answers: %s
                """.formatted(
                lengthRule,
                level,
                safe(title),
                safe(oneLineReason),
                safeToJsonList(ruleReasons),
                safeToJson(answers)
        );
    }

    private String safe(String s) {
        return s == null ? "" : s.replace("\n", " ").trim();
    }

    private String safeToJson(Map<String, Object> map) {
        try {
            if (map == null) return "{}";
            return om.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String safeToJsonList(List<String> list) {
        try {
            if (list == null) return "[]";
            return om.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private ExplainDecisionResponse parseJson(String text) {
        if (text == null) return null;

        int s = text.indexOf('{');
        int e = text.lastIndexOf('}');
        if (s >= 0 && e > s) text = text.substring(s, e + 1);

        try {
            JsonNode root = om.readTree(text);
            String summary = root.path("summary").asText(null);
            String detail = root.path("detail").asText("");

            List<String> bullets = new ArrayList<>();
            JsonNode b = root.path("bullets");
            if (b.isArray()) for (JsonNode n : b) bullets.add(n.asText());

            List<String> ask = new ArrayList<>();
            JsonNode a = root.path("ask");
            if (a.isArray()) for (JsonNode n : a) ask.add(n.asText());

            return new ExplainDecisionResponse(summary, detail, bullets, ask);
        } catch (Exception e2) {
            return null;
        }
    }
}