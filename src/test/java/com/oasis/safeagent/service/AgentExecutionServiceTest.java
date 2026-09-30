package com.oasis.safeagent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class AgentExecutionServiceTest {

    @Test
    void rejectedModificationMustNotChangeFile() throws Exception {

        ToolExecutionService toolExecutionService =
                new ToolExecutionService();

        EvidenceValidatorService evidenceValidatorService =
                new EvidenceValidatorService(toolExecutionService);

        AgentExecutionService service =
                new AgentExecutionService(
                        new RiskAssessmentService(),
                        new SafetyGatewayService(),
                        toolExecutionService,
                        evidenceValidatorService,
                        new AuditService()
                );

        String originalContent =
                "server.port=8080\n"
                + "logging.level.root=DEBUG\n";

        Path filePath =
                toolExecutionService.saveFile(
                        "rejected-demo.properties",
                        originalContent.getBytes(StandardCharsets.UTF_8)
                );

        String result =
                service.executeApprovedModification(
                        filePath,
                        "logging.level.root",
                        "INFO",
                        "REJECTED"
                );

        assertEquals(
                "EXECUTION_REJECTED",
                result
        );

        String actual =
                toolExecutionService.readConfig(filePath);

        assertTrue(
                actual.contains(
                        "logging.level.root=DEBUG"
                )
        );
    }

    @Test
    void approvedModificationMustChangeAndVerifyFile() throws Exception {

        ToolExecutionService toolExecutionService =
                new ToolExecutionService();

        EvidenceValidatorService evidenceValidatorService =
                new EvidenceValidatorService(toolExecutionService);

        AgentExecutionService service =
                new AgentExecutionService(
                        new RiskAssessmentService(),
                        new SafetyGatewayService(),
                        toolExecutionService,
                        evidenceValidatorService,
                        new AuditService()
                );

        String originalContent =
                "server.port=8080\n"
                + "logging.level.root=DEBUG\n";

        Path filePath =
                toolExecutionService.saveFile(
                        "approved-demo.properties",
                        originalContent.getBytes(StandardCharsets.UTF_8)
                );

        String result =
                service.executeApprovedModification(
                        filePath,
                        "logging.level.root",
                        "INFO",
                        "APPROVED"
                );

        assertTrue(
                result.contains(
                        "MODIFIED: logging.level.root=INFO"
                )
        );

        assertTrue(
                result.contains(
                        "EVIDENCE: VERIFIED"
                )
        );

        String actual =
                toolExecutionService.readConfig(filePath);

        assertTrue(
                actual.contains(
                        "logging.level.root=INFO"
                )
        );
    }
}