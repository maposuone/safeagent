package com.oasis.safeagent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class EvidenceValidatorServiceTest {

    @Test
    void shouldVerifyActualModifiedFile() throws Exception {

        ToolExecutionService toolExecutionService =
                new ToolExecutionService();

        EvidenceValidatorService evidenceValidatorService =
                new EvidenceValidatorService(toolExecutionService);

        String originalContent =
                "server.port=8080\n"
                + "logging.level.root=DEBUG\n";

        Path filePath =
                toolExecutionService.saveFile(
                        "evidence-demo.properties",
                        originalContent.getBytes(StandardCharsets.UTF_8)
                );

        // 実際にファイル変更
        toolExecutionService.modifyConfig(
                filePath,
                "logging.level.root",
                "INFO"
        );

        // 正しい期待値
        String verified =
                evidenceValidatorService.verifyConfigValue(
                        filePath,
                        "logging.level.root",
                        "INFO"
                );

        assertEquals(
                "VERIFIED",
                verified
        );

        // 間違った期待値
        String unverified =
                evidenceValidatorService.verifyConfigValue(
                        filePath,
                        "logging.level.root",
                        "DEBUG"
                );

        assertEquals(
                "UNVERIFIED",
                unverified
        );
    }
}