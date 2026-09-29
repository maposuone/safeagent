package com.oasis.safeagent.service;

import org.springframework.stereotype.Service;

@Service
public class ApprovalService {

    public String approve(String action) {
        if (action == null || action.isBlank()) {
            return "REJECTED";
        }

        return "APPROVED";
    }

    public String reject(String action) {
        return "REJECTED";
    }
}