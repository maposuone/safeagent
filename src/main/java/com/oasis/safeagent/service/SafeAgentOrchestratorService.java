package com.oasis.safeagent.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class SafeAgentOrchestratorService {

    private final AgentDecisionService agentDecisionService;
    private final RiskAssessmentService riskAssessmentService;
    private final SafetyGatewayService safetyGatewayService;
    private final AgentExecutionService agentExecutionService;

    public SafeAgentOrchestratorService(
            AgentDecisionService agentDecisionService,
            RiskAssessmentService riskAssessmentService,
            SafetyGatewayService safetyGatewayService,
            AgentExecutionService agentExecutionService) {

        this.agentDecisionService = agentDecisionService;
        this.riskAssessmentService = riskAssessmentService;
        this.safetyGatewayService = safetyGatewayService;
        this.agentExecutionService = agentExecutionService;
    }

    public Map<String, Object> decideAndEvaluate(
            String userRequest,
            String fileContent,
            String analysisResult) {

        String action =
                agentDecisionService.decideNextAction(
                        userRequest,
                        fileContent,
                        analysisResult
                );

        String riskLevel =
                riskAssessmentService.assess(action);

        String decision =
                safetyGatewayService.evaluate(
                        action,
                        riskLevel
                );

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put("action", action);
        result.put("riskLevel", riskLevel);
        result.put("decision", decision);

        return result;
    }

    public Map<String, Object> executeApprovedModification(
            Path filePath,
            String key,
            String newValue,
            String approvalStatus) throws IOException {

        String executionResult =
                agentExecutionService.executeApprovedModification(
                        filePath,
                        key,
                        newValue,
                        approvalStatus
                );

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put("action", "MODIFY_CONFIG");
        result.put("approvalStatus", approvalStatus);
        result.put("executionResult", executionResult);

        return result;
    }
}