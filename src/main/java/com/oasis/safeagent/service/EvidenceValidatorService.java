package com.oasis.safeagent.service;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.stereotype.Service;

@Service
public class EvidenceValidatorService {

    private final ToolExecutionService toolExecutionService;

    public EvidenceValidatorService(
            ToolExecutionService toolExecutionService) {

        this.toolExecutionService = toolExecutionService;
    }

    public String verifyConfigValue(
            Path filePath,
            String key,
            String expectedValue) throws IOException {

        boolean verified =
                toolExecutionService.verifyValue(
                        filePath,
                        key,
                        expectedValue
                );

        if (verified) {
            return "VERIFIED";
        }

        return "UNVERIFIED";
    }
}