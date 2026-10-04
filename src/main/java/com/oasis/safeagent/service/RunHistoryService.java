package com.oasis.safeagent.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RunHistoryService {

    private final JdbcTemplate jdbcTemplate;

    public RunHistoryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /*
     * /api/safeagent/run の結果を保存する。
     */
    public UUID saveInitialRun(
            String fileName,
            String userRequest,
            Map<String, Object> result) {

        UUID runId = UUID.randomUUID();

        String requestMode =
                value(
                        result,
                        "mode",
                        value(result, "requestMode", "UNKNOWN")
                );
        String status =
                value(result, "status", "UNKNOWN");

        String approvalStatus =
                "WAITING_FOR_APPROVAL".equals(status)
                        ? "PENDING"
                        : "NOT_REQUIRED";

        String sql = """
                INSERT INTO agent_run (
                    run_id,
                    file_name,
                    user_request,
                    request_mode,
                    first_action,
                    first_risk_level,
                    first_decision,
                    second_action,
                    second_risk_level,
                    second_decision,
                    approval_status,
                    execution_result,
                    evidence_status,
                    final_status,
                    analysis_summary
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(
                sql,
                runId,
                safe(fileName),
                safe(userRequest),
                requestMode,

                value(result, "firstAction", null),
                value(result, "firstRiskLevel", null),
                value(result, "firstDecision", null),

                value(result, "secondAction", null),
                value(result, "secondRiskLevel", null),
                value(result, "secondDecision", null),

                approvalStatus,
                null,
                "NOT_CHECKED",
                status,

                value(result, "analysisSummary", null)
        );

        /*
         * EXPLAIN / ANALYZE / BLOCKEDなど、
         * 承認待ちでない処理はここで完了時刻を付ける。
         */
        if (!"WAITING_FOR_APPROVAL".equals(status)) {

            jdbcTemplate.update(
                    """
                    UPDATE agent_run
                    SET completed_at = now()
                    WHERE run_id = ?
                    """,
                    runId
            );
        }

        return runId;
    }

    /*
     * Secret / Prompt Injectionで
     * Gemini実行前にBLOCKされた履歴。
     */
    public UUID saveBlockedRun(
            String fileName,
            String userRequest,
            String reason) {

        UUID runId = UUID.randomUUID();

        String sql = """
                INSERT INTO agent_run (
                    run_id,
                    completed_at,
                    file_name,
                    user_request,
                    request_mode,
                    approval_status,
                    execution_result,
                    evidence_status,
                    final_status,
                    analysis_summary
                )
                VALUES (
                    ?,
                    now(),
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?
                )
                """;

        jdbcTemplate.update(
                sql,
                runId,
                safe(fileName),
                safe(userRequest),
                "UNKNOWN",
                "NOT_APPLICABLE",
                "BLOCKED",
                "NOT_CHECKED",
                "BLOCKED",
                reason
        );

        return runId;
    }

    /*
     * Human Approval後に、
     * 同じagent_runを更新する。
     */
    public void updateAfterApproval(
            UUID runId,
            Map<String, Object> result) {

        String sql = """
                UPDATE agent_run
                SET
                    completed_at = now(),
                    approval_status = ?,
                    execution_result = ?,
                    evidence_status = ?,
                    final_status = ?
                WHERE run_id = ?
                """;

        int updated =
                jdbcTemplate.update(
                        sql,
                        value(
                                result,
                                "approvalStatus",
                                "UNKNOWN"
                        ),
                        value(
                                result,
                                "executionResult",
                                "UNKNOWN"
                        ),
                        value(
                                result,
                                "evidence",
                                "NOT_CHECKED"
                        ),
                        value(
                                result,
                                "status",
                                "UNKNOWN"
                        ),
                        runId
                );

        if (updated != 1) {
            throw new IllegalStateException(
                    "履歴を更新できませんでした。runId=" + runId
            );
        }
    }

    private String value(
            Map<String, Object> result,
            String key,
            String defaultValue) {

        Object value = result.get(key);

        if (value == null) {
            return defaultValue;
        }

        String text = value.toString();

        if (text.isBlank()) {
            return defaultValue;
        }

        return text;
    }

    private String safe(String value) {

        if (value == null) {
            return "";
        }

        return value;
    }

    public List<Map<String, Object>> findHistory(
            int page,
            int size) {

        int offset = page * size;

        String sql = """
                SELECT
                    run_id,
                    created_at,
                    file_name,
                    user_request,
                    request_mode,
                    COALESCE(second_action, first_action) AS action,
                    COALESCE(second_risk_level, first_risk_level) AS risk_level,
                    COALESCE(second_decision, first_decision) AS decision,
                    approval_status,
                    execution_result,
                    evidence_status,
                    final_status
                FROM agent_run
                ORDER BY created_at DESC, id DESC
                LIMIT ?
                OFFSET ?
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {

                    Map<String, Object> row =
                            new LinkedHashMap<>();

                    row.put(
                            "runId",
                            rs.getObject("run_id")
                    );

                    row.put(
                            "createdAt",
                            rs.getTimestamp("created_at")
                    );

                    row.put(
                            "fileName",
                            rs.getString("file_name")
                    );

                    row.put(
                            "userRequest",
                            rs.getString("user_request")
                    );

                    row.put(
                            "requestMode",
                            rs.getString("request_mode")
                    );

                    row.put(
                            "action",
                            rs.getString("action")
                    );

                    row.put(
                            "riskLevel",
                            rs.getString("risk_level")
                    );

                    row.put(
                            "decision",
                            rs.getString("decision")
                    );

                    row.put(
                            "approvalStatus",
                            rs.getString("approval_status")
                    );

                    row.put(
                            "executionResult",
                            rs.getString("execution_result")
                    );

                    row.put(
                            "evidenceStatus",
                            rs.getString("evidence_status")
                    );

                    row.put(
                            "finalStatus",
                            rs.getString("final_status")
                    );

                    return row;
                },
                size,
                offset
        );
    }

    public long countHistory() {

        Long count =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM agent_run",
                        Long.class
                );

        return count == null ? 0 : count;
    }
}