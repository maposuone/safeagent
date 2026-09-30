package com.oasis.safeagent.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private volatile Map<String, Object> latestAudit =
            Map.of();

    public void record(
            String action,
            String riskLevel,
            String decision,
            String approvalStatus,
            String executionResult,
            String evidenceResult) {

        Map<String, Object> audit =
                new LinkedHashMap<>();

        audit.put(
                "timestamp",
                Instant.now().toString()
        );

        audit.put(
                "action",
                safe(action)
        );

        audit.put(
                "riskLevel",
                safe(riskLevel)
        );

        audit.put(
                "decision",
                safe(decision)
        );

        audit.put(
                "approvalStatus",
                safe(approvalStatus)
        );

        audit.put(
                "executionResult",
                safe(executionResult)
        );

        audit.put(
                "evidenceResult",
                safe(evidenceResult)
        );

        /*
         * デモ表示用に直近Auditを保持
         */
        latestAudit =
                new LinkedHashMap<>(audit);

        /*
         * Cloud Runでは標準出力が
         * Cloud Loggingへ送られる。
         */
        System.out.println(
                "[SAFEAGENT_AUDIT] "
                + toJson(audit)
        );
    }

    public Map<String, Object> getLatestAudit() {

        return new LinkedHashMap<>(
                latestAudit
        );
    }

    private String safe(String value) {

        if (value == null) {
            return "";
        }

        return value;
    }

    private String toJson(
            Map<String, Object> audit) {

        StringBuilder json =
                new StringBuilder("{");

        boolean first = true;

        for (Map.Entry<String, Object> entry
                : audit.entrySet()) {

            if (!first) {
                json.append(",");
            }

            json.append("\"")
                .append(escape(entry.getKey()))
                .append("\":\"")
                .append(
                        escape(
                                String.valueOf(
                                        entry.getValue()
                                )
                        )
                )
                .append("\"");

            first = false;
        }

        json.append("}");

        return json.toString();
    }

    private String escape(String value) {

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}