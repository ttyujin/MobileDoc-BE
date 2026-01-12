package com.mobiledoc.mobiledocbackend.alerts;

import com.mobiledoc.mobiledocbackend.alerts.dto.EmergencyAlertRequest;
import com.mobiledoc.mobiledocbackend.auth.MailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/alerts")
public class EmergencyAlertController {

    private final MailService mailService;

    @Value("${app.alert.adminEmail:}")
    private String adminEmail;

    public EmergencyAlertController(MailService mailService) {
        this.mailService = mailService;
    }

    @PostMapping("/emergency")
    public ResponseEntity<?> sendEmergency(@RequestBody EmergencyAlertRequest req) {
        // 1) 받을 사람 목록 만들기 (선택한 사람 + 관리자)
        Set<String> recipients = new LinkedHashSet<>();

        String contactEmail = safe(req.getTarget() != null ? req.getTarget().getEmail() : null);
        if (!contactEmail.isEmpty()) recipients.add(contactEmail);

        String admin = safe(adminEmail);
        if (!admin.isEmpty()) recipients.add(admin);

        if (recipients.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "message", "No recipient email. (target.email or app.alert.adminEmail required)"
            ));
        }

        // 2) 메일 제목/본문 만들기
        String reporterName = safe(req.getReporter() != null ? req.getReporter().getName() : null);
        if (reporterName.isEmpty()) reporterName = "접속자";

        String reporterEmail = safe(req.getReporter() != null ? req.getReporter().getEmail() : null);


        String targetName = safe(req.getTarget() != null ? req.getTarget().getName() : null);
        String targetPhone = safe(req.getTarget() != null ? req.getTarget().getPhone() : null);

        String subject = "[MobileDoc] 위기상황 알림 - " + reporterName;

        StringBuilder body = new StringBuilder();

        body.append("\n[상태 요약]\n");
        body.append(safe(req.getMessage())).append("\n");

        body.append("[접속자 정보]\n");
        body.append("- 이름: ").append(reporterName).append("\n");
        if (!reporterEmail.isEmpty()) body.append("- 이메일: ").append(reporterEmail).append("\n");

        body.append("\n[선택한 연락처]\n");
        if (!targetName.isEmpty()) body.append("- 이름: ").append(targetName).append("\n");
        if (!contactEmail.isEmpty()) body.append("- 이메일: ").append(contactEmail).append("\n");
        if (!targetPhone.isEmpty()) body.append("- 전화: ").append(targetPhone).append("\n");


        // 3) 발송 (각 수신자에게 개별 발송)
        List<String> sent = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        for (String to : recipients) {
            boolean ok = mailService.trySendText(to, subject, body.toString());
            if (ok) sent.add(to);
            else failed.add(to);
        }

        return ResponseEntity.ok(Map.of(
                "ok", failed.isEmpty(),
                "sent", sent,
                "failed", failed
        ));
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
