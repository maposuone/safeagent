package com.oasis.safeagent.service;

import org.springframework.stereotype.Service;

@Service
public class AgentExecutionService {

    private final RiskAssessmentService riskAssessmentService;
    private final SafetyGatewayService safetyGatewayService;
    private final ToolExecutionService toolExecutionService;

    public AgentExecutionService(
            RiskAssessmentService riskAssessmentService,
            SafetyGatewayService safetyGatewayService,
            ToolExecutionService toolExecutionService) {

        this.riskAssessmentService = riskAssessmentService;
        this.safetyGatewayService = safetyGatewayService;
        this.toolExecutionService = toolExecutionService;
    }

    public String execute(String action) {

        String riskLevel = riskAssessmentService.assess(action);
        String decision = safetyGatewayService.evaluate(action, riskLevel);

        if ("ALLOW".equals(decision)) {
            return toolExecutionService.execute(action);
        }

        if ("APPROVAL_REQUIRED".equals(decision)) {
            return "EXECUTION_WAITING_FOR_APPROVAL";
        }

        return "EXECUTION_BLOCKED";
    }
}