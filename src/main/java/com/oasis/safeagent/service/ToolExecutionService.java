package com.oasis.safeagent.service;

import org.springframework.stereotype.Service;

@Service
public class ToolExecutionService {

    public String execute(String action) {

        if (action == null || action.isBlank()) {
            return "EXECUTION_REJECTED";
        }

        return switch (action.toUpperCase()) {

            case "READ_CONFIG" ->
                "EXECUTED: READ_CONFIG";

            case "ANALYZE_CONFIG" ->
                "EXECUTED: ANALYZE_CONFIG";

            case "COMPARE_CONFIG" ->
                "EXECUTED: COMPARE_CONFIG";

            case "MODIFY_CONFIG" ->
                "EXECUTED: MODIFY_CONFIG";

            default ->
                "EXECUTION_BLOCKED";
        };
    }
}