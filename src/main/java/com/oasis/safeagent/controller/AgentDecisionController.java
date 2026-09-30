package com.oasis.safeagent.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oasis.safeagent.service.AgentDecisionService;

@RestController
@RequestMapping("/api/agent")
public class AgentDecisionController {

    private final AgentDecisionService agentDecisionService;

    public AgentDecisionController(
            AgentDecisionService agentDecisionService) {

        this.agentDecisionService = agentDecisionService;
    }

    @PostMapping("/decide")
    public String decide(
            @RequestParam String request,
            @RequestParam String content,
            @RequestParam(required = false) String analysisResult) {

        return agentDecisionService.decideNextAction(
                request,
                content,
                analysisResult
        );
    }
}