package com.oasis.safeagent.service;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.stereotype.Service;

@Service
public class AgentExecutionService {

    private final RiskAssessmentService riskAssessmentService;
    private final SafetyGatewayService safetyGatewayService;
    private final ToolExecutionService toolExecutionService;
    private final EvidenceValidatorService evidenceValidatorService;
    private final AuditService auditService;

    public AgentExecutionService(
            RiskAssessmentService riskAssessmentService,
            SafetyGatewayService safetyGatewayService,
            ToolExecutionService toolExecutionService,
            EvidenceValidatorService evidenceValidatorService,
            AuditService auditService) {

        this.riskAssessmentService = riskAssessmentService;
        this.safetyGatewayService = safetyGatewayService;
        this.toolExecutionService = toolExecutionService;
        this.evidenceValidatorService = evidenceValidatorService;
        this.auditService = auditService;
    }

    public String execute(String action) {

        String riskLevel =
                riskAssessmentService.assess(action);

        String decision =
                safetyGatewayService.evaluate(
                        action,
                        riskLevel
                );

        if ("ALLOW".equals(decision)) {

            String executionResult =
                    toolExecutionService.execute(action);

            auditService.record(
                    action,
                    riskLevel,
                    decision,
                    "NOT_REQUIRED",
                    executionResult,
                    "NOT_CHECKED"
            );

            return executionResult;
        }

        if ("APPROVAL_REQUIRED".equals(decision)) {

            String result =
                    "EXECUTION_WAITING_FOR_APPROVAL";

            auditService.record(
                    action,
                    riskLevel,
                    decision,
                    "PENDING",
                    result,
                    "NOT_CHECKED"
            );

            return result;
        }

        String result =
                "EXECUTION_BLOCKED";

        auditService.record(
                action,
                riskLevel,
                decision,
                "NOT_APPLICABLE",
                result,
                "NOT_CHECKED"
        );

        return result;
    }

    public String executeApprovedModification(
            Path filePath,
            String key,
            String newValue,
            String approvalStatus) throws IOException {

        String action = "MODIFY_CONFIG";

        String riskLevel =
                riskAssessmentService.assess(action);

        String decision =
                safetyGatewayService.evaluate(
                        action,
                        riskLevel
                );

        /*
         * Safety Gatewayが
         * APPROVAL_REQUIREDと判断していない場合も実行しない。
         */
        if (!"APPROVAL_REQUIRED".equals(decision)) {

            auditService.record(
                    action,
                    riskLevel,
                    decision,
                    approvalStatus,
                    "EXECUTION_BLOCKED",
                    "NOT_CHECKED"
            );

            return "EXECUTION_BLOCKED";
        }

        /*
         * 人間が明示的にAPPROVEDしていない限り実行しない。
         */
        if (!"APPROVED".equals(approvalStatus)) {

            auditService.record(
                    action,
                    riskLevel,
                    decision,
                    approvalStatus,
                    "EXECUTION_REJECTED",
                    "NOT_CHECKED"
            );

            return "EXECUTION_REJECTED";
        }

        /*
         * ここで初めて実ファイルを変更する。
         */
        String executionResult =
                toolExecutionService.modifyConfig(
                        filePath,
                        key,
                        newValue
                );

        /*
         * Toolが失敗した場合はEvidence検証へ進まない。
         */
        if (!executionResult.startsWith("MODIFIED:")) {

            auditService.record(
                    action,
                    riskLevel,
                    decision,
                    approvalStatus,
                    executionResult,
                    "NOT_CHECKED"
            );

            return executionResult;
        }

        /*
         * AIの「変更した」という主張ではなく、
         * 実ファイルを再読込して確認する。
         */
        String evidenceResult =
                evidenceValidatorService.verifyConfigValue(
                        filePath,
                        key,
                        newValue
                );

        /*
         * 最後にAuditへ記録。
         */
        auditService.record(
                action,
                riskLevel,
                decision,
                approvalStatus,
                executionResult,
                evidenceResult
        );

        return executionResult
                + " | EVIDENCE: "
                + evidenceResult;
    }

    /*
     * 場所指定付き変更。
     * 同じキーが複数存在しても LINE:n または XML selector で対象を特定する。
     */
    public String executeApprovedModification(
            Path filePath,
            String target,
            String key,
            String expectedCurrentValue,
            String newValue,
            String approvalStatus) throws IOException {

        String action = "MODIFY_CONFIG";

        String riskLevel =
                riskAssessmentService.assess(action);

        String decision =
                safetyGatewayService.evaluate(
                        action,
                        riskLevel
                );

        if (!"APPROVAL_REQUIRED".equals(decision)) {
            auditService.record(
                    action,
                    riskLevel,
                    decision,
                    approvalStatus,
                    "EXECUTION_BLOCKED",
                    "NOT_CHECKED"
            );

            return "EXECUTION_BLOCKED";
        }

        if (!"APPROVED".equals(approvalStatus)) {
            auditService.record(
                    action,
                    riskLevel,
                    decision,
                    approvalStatus,
                    "EXECUTION_REJECTED",
                    "NOT_CHECKED"
            );

            return "EXECUTION_REJECTED";
        }

        String executionResult =
                toolExecutionService.modifyConfigAtTarget(
                        filePath,
                        target,
                        key,
                        expectedCurrentValue,
                        newValue
                );

        if (!executionResult.startsWith("MODIFIED:")) {
            auditService.record(
                    action,
                    riskLevel,
                    decision,
                    approvalStatus,
                    executionResult,
                    "NOT_CHECKED"
            );

            return executionResult;
        }

        String evidenceResult =
                toolExecutionService.verifyValueAtTarget(
                        filePath,
                        target,
                        key,
                        newValue
                )
                        ? "VERIFIED"
                        : "UNVERIFIED";

        auditService.record(
                action,
                riskLevel,
                decision,
                approvalStatus,
                executionResult,
                evidenceResult
        );

        return executionResult
                + " | EVIDENCE: "
                + evidenceResult;
    }

}