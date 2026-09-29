package com.oasis.safeagent.service;

import org.springframework.stereotype.Service;

@Service
public class SafetyGatewayService {

    public String evaluate(String action, String riskLevel) {

        if (action == null || riskLevel == null) {
            return "BLOCK";
        }

        if ("CRITICAL".equalsIgnoreCase(riskLevel)) {
            return "BLOCK";
        }

        if ("HIGH".equalsIgnoreCase(riskLevel)) {
            return "APPROVAL_REQUIRED";
        }

        if ("LOW".equalsIgnoreCase(riskLevel)
                || "MEDIUM".equalsIgnoreCase(riskLevel)) {
            return "ALLOW";
        }

        return "BLOCK";
    }
}