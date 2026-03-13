package com.mobiledoc.mobiledocbackend.alerts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobiledoc.mobiledocbackend.ai.OpenAiClientFacade;
import com.mobiledoc.mobiledocbackend.alerts.dto.ChecklistSummaryEmailRequest;
import com.mobiledoc.mobiledocbackend.alerts.dto.ChecklistSummaryEmailResponse;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ChecklistSummaryEmailService {

    // ✅ 2개 모두 주입(지메일 → 네이버 순서로 시도)
    private final JavaMailSender gmailMailSender;
    private final JavaMailSender naverMailSender;

    private final ObjectMapper om = new ObjectMapper();

    public ChecklistSummaryEmailService(
            @Qualifier("gmailMailSender") JavaMailSender gmailMailSender,
            @Qualifier("naverMailSender") JavaMailSender naverMailSender
    ) {
        this.gmailMailSender = gmailMailSender;
        this.naverMailSender = naverMailSender;
    }

    private boolean hasOpenAiKey() {
        String key = System.getenv("OPENAI_API_KEY");
        return key != null && !key.isBlank();
    }

    public ChecklistSummaryEmailResponse send(ChecklistSummaryEmailRequest req) {
        if (req == null || req.toEmail == null || req.toEmail.isBlank()) {
            return new ChecklistSummaryEmailResponse(false, "toEmail(수신자 이메일)이 비어있어요.");
        }

        try {
            String subject = "[MobileDoc] 진료용 요약(1분 판별 + 3분 체크리스트)";
            String body = buildBody(req);

            // ✅ Gmail 먼저 시도 → 실패하면 Naver로 재시도
            sendMailWithFallback(req.toEmail.trim(), subject, body);

            return new ChecklistSummaryEmailResponse(true, "메일 전송 완료");
        } catch (Exception e) {
            return new ChecklistSummaryEmailResponse(false, "메일 전송 실패: " + e.getMessage());
        }
    }

    private String buildBody(ChecklistSummaryEmailRequest req) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        String decisionJson = safeJson(req.decision);
        String answersJson = safeJson(req.answers);
        String checklistJson = safeJson(req.checklist);
        String contextJson = safeJson(req.context);

        // ✅ 키 없으면 템플릿(원본 포함)
        if (!hasOpenAiKey()) {
            return """
                    MobileDoc 진료용 요약 (자동 생성)
                    생성 시각: %s

                    [1분 판별 결과]
                    %s

                    [1분 답변(원본)]
                    %s

                    [3분 준비 체크리스트(원본)]
                    %s

                    [참고 컨텍스트]
                    %s

                    ----
                    안내: 본 요약은 사용자가 입력한 정보를 정리한 것이며, 의료 진단/처방을 대신하지 않습니다.
                    """.formatted(now, decisionJson, answersJson, checklistJson, contextJson);
        }

        // ✅ "길고 세세한" 진료 준비 문서형 프롬프트(날조 금지/미기록 처리/의사 예상질문 포함)
        String prompt = """
                너는 '원격/대면 진료 전에 의사에게 전달할 준비 문서'를 작성한다. 한국어로 작성해라.
                의료 진단/처방/약 추천은 절대 하지 마라.
                아래 JSON에 있는 사실만 사용해라(날조 금지). 없으면 반드시 "미기록"이라고 써라.

                특히 지켜라:
                - 사용자 입력을 "의사가 빠르게 판단"할 수 있게 구조화
                - 체크리스트(JSON)는 배열이며 각 항목이 보통 {id,type,label/text,checked,value} 형태다.
                  checked=true 또는 value가 입력된 항목만 근거로 반영
                - '비대면 vs 대면'은 단정하지 말고, 기준을 제시하고 "현재 입력으로는 ~가 부족하다"를 명확히 써라.
                - 아래에 '의사 예상 질문' 섹션을 만들고, 부족한 정보는 질문으로 채우게 해라.
                - 출력은 텍스트만. 코드블록/JSON 금지.

                출력 형식:

                MobileDoc 진료용 요약 (AI 자동 정리)
                생성 시각: %s

                [1) 의사에게 드리는 한 줄 요약]
                - (주증상 + 기간 + 악화/완화 + 현재 가장 불편한 점)

                [2) 현재 상태 요약(핵심만)]
                - 시작: (미기록이면 미기록)
                - 악화/완화: (미기록이면 미기록)
                - 심각도/일상: (있으면)
                - 동반 증상: (입력에 있으면만, 없으면 미기록)

                [3) 비대면/대면 판단을 위한 체크]
                - 비대면으로 시작해도 되는 조건(입력 근거로)
                - 대면이 더 안전한 조건(입력 근거로)
                - 현재 입력에서 부족한 정보(미기록 항목) 3~6개

                [4) 의사 예상 질문(환자가 미리 답하면 좋은 것)]
                - 예/아니오로 답할 질문 6개
                - 숫자/사실로 답할 질문 6개
                (예: 최고 체온, 호흡곤란 여부, 흉통 여부, 기침/가래 색, 코로나/독감 검사 여부, 기저질환 여부 등)

                [5) 환자가 그대로 읽을 30초 설명 스크립트]
                - "저는 ___ 때문에 상담받고 싶습니다. ___부터 시작됐고, 현재 ___이며, 특히 ___가 불편합니다. 제가 원하는 것은 ___입니다."

                [6) 병원에 확인할 질문(정확히 3개)]
                1) ...
                2) ...
                3) ...

                [7) 안전 안내(1~2문장)]
                - 악화/응급 징후가 있으면 즉시 대면/응급실/119 고려(진단 없이)

                아래 JSON은 참고 데이터다. 이 범위 밖 정보는 절대 추가하지 마라.

                [decision]
                %s

                [answers]
                %s

                [checklist]
                %s

                [context]
                %s
                """.formatted(now, decisionJson, answersJson, checklistJson, contextJson);

        // ✅ 더 길게(세세하게) 나오도록 토큰 증가
        String aiText = OpenAiClientFacade.callResponsesApi(prompt, 1600);

        if (aiText == null || aiText.isBlank()) {
            return """
                    MobileDoc 진료용 요약 (AI 실패 → 템플릿)
                    생성 시각: %s

                    [1분 판별 결과]
                    %s

                    [3분 준비 체크리스트]
                    %s

                    ----
                    안내: 본 요약은 사용자가 입력한 정보를 정리한 것이며, 의료 진단/처방을 대신하지 않습니다.
                    """.formatted(now, decisionJson, checklistJson);
        }

        // ✅ 혹시 안내문구가 빠져도 서버에서 보장
        String trimmed = aiText.trim();
        if (!trimmed.contains("의료 진단") && !trimmed.contains("진단/처방")) {
            trimmed = trimmed + "\n\n----\n안내: 본 요약은 사용자가 입력한 정보를 정리한 것이며, 의료 진단/처방을 대신하지 않습니다.\n";
        }

        return trimmed;
    }

    private String safeJson(Object obj) {
        try {
            if (obj == null) return "{}";
            return om.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private void sendMailWithFallback(String to, String subject, String body) throws Exception {
        Exception first = null;

        try {
            sendMail(gmailMailSender, to, subject, body);
            return;
        } catch (Exception e) {
            first = e;
        }

        try {
            sendMail(naverMailSender, to, subject, body);
        } catch (Exception e2) {
            String msg = "Gmail 실패: " + (first != null ? first.getMessage() : "") +
                    " / Naver 실패: " + e2.getMessage();
            throw new Exception(msg, e2);
        }
    }

    private void sendMail(JavaMailSender sender, String to, String subject, String body) throws Exception {
        MimeMessage msg = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, false);

        // ✅ setFrom을 일부러 안 함 (환경변수 없어도 앱이 안 죽게)
        sender.send(msg);
    }
}