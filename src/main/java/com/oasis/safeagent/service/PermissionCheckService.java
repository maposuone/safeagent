package com.oasis.safeagent.service;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class PermissionCheckService {

    private static final List<String> ALLOWED_ACTIONS = List.of(
            "READ_CONFIG",
            "ANALYZE_CONFIG",
            "COMPARE_CONFIG"
    );

    public boolean isAllowed(String action) {
        if (action == null) {
            return false;
        }

        return ALLOWED_ACTIONS.contains(action.toUpperCase());
    }
}