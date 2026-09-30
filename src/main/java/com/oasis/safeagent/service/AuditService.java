package com.oasis.safeagent.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

@Service
public class AuditService {

    public void record(
            String action,
            String riskLevel,
            String decision,
            String approvalStatus,
            String executionResult,
            String evidenceResult) {

        String log =
                "{"
                + "\"timestamp\":\"" + Instant.now() + "\","
                + "\"action\":\"" + safe(action) + "\","
                + "\"riskLevel\":\"" + safe(riskLevel) + "\","
                + "\"decision\":\"" + safe(decision) + "\","
                + "\"approvalStatus\":\"" + safe(approvalStatus) + "\","
                + "\"executionResult\":\"" + safe(executionResult) + "\","
                + "\"evidenceResult\":\"" + safe(evidenceResult) + "\""
                + "}";

        System.out.println("[SAFEAGENT_AUDIT] " + log);
    }

    private String safe(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}