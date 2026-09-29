package com.oasis.safeagent.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oasis.safeagent.service.AgentExecutionService;

@RestController
@RequestMapping("/api/agent")
public class AgentExecutionController {

    private final AgentExecutionService agentExecutionService;

    public AgentExecutionController(AgentExecutionService agentExecutionService) {
        this.agentExecutionService = agentExecutionService;
    }

    @PostMapping("/execute")
    public String execute(@RequestParam String action) {
        return agentExecutionService.execute(action);
    }
}