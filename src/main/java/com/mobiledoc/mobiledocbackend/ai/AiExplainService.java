package com.mobiledoc.mobiledocbackend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobiledoc.mobiledocbackend.ai.dto.ExplainDecisionRequest;
import com.mobiledoc.mobiledocbackend.ai.dto.ExplainDecisionResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiExplainService {

    private final ObjectMapper om = new ObjectMapper();

    private boolean hasOpenAiKey() {
        String key = System.getenv("OPENAI_API_KEY");
        return key != null && !key.isBlank();
    }

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
