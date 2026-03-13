package com.mobiledoc.mobiledocbackend.alerts;

import com.mobiledoc.mobiledocbackend.alerts.dto.ChecklistSummaryEmailRequest;
import com.mobiledoc.mobiledocbackend.alerts.dto.ChecklistSummaryEmailResponse;
import com.mobiledoc.mobiledocbackend.stats.SymptomLogService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/alerts")
public class ChecklistAlertController {

    private final ChecklistSummaryEmailService checklistSummaryEmailService;

    // ✅ 추가: 3분 체크 완료 시 증상/날짜 기록 저장용 서비스
    private final SymptomLogService symptomLogService;

    public ChecklistAlertController(
            ChecklistSummaryEmailService checklistSummaryEmailService,
            SymptomLogService symptomLogService
    ) {
        this.checklistSummaryEmailService = checklistSummaryEmailService;
        this.symptomLogService = symptomLogService;
    }

    @PostMapping("/checklist")
    public ChecklistSummaryEmailResponse sendChecklist(@RequestBody ChecklistSummaryEmailRequest req) {

        // ✅ 추가: 기록 저장(실패해도 메일 전송은 계속 진행)
        try {
            symptomLogService.recordFromChecklist(req);
        } catch (Exception ignored) {}

        // ✅ 기존: 메일 전송
        return checklistSummaryEmailService.send(req);
    }
}