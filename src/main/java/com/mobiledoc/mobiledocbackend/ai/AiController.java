package com.mobiledoc.mobiledocbackend.ai;

import com.mobiledoc.mobiledocbackend.ai.dto.ChatRequest;
import com.mobiledoc.mobiledocbackend.ai.dto.ChatResponse;
import com.mobiledoc.mobiledocbackend.ai.dto.ExplainDecisionRequest;
import com.mobiledoc.mobiledocbackend.ai.dto.ExplainDecisionResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiExplainService aiExplainService;

    public AiController(AiExplainService aiExplainService) {
        this.aiExplainService = aiExplainService;
    }

    @PostMapping("/explain-decision")
    public ExplainDecisionResponse explain(@RequestBody ExplainDecisionRequest req) {
        return aiExplainService.explain(req);
    }

    // ✅ 챗봇 엔드포인트 (프론트 1순위)
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest req) {
        return aiExplainService.chat(req);
    }
}