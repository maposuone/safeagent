package com.oasis.safeagent.service;

import org.springframework.stereotype.Service;

@Service
public class RiskAssessmentService {

    public String assess(String action) {

        if (action == null) {
            return "CRITICAL";
        }

        return switch (action.toUpperCase()) {
            case "READ_CONFIG", "ANALYZE_CONFIG" -> "LOW";
            case "COMPARE_CONFIG" -> "MEDIUM";
            case "MODIFY_CONFIG" -> "HIGH";
            case "DELETE_CONFIG" -> "CRITICAL";
            default -> "CRITICAL";
        };
    }
}