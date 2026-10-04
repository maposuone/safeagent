package com.oasis.safeagent.model;

public record ConfigDifference(
        int no,
        String type,
        String targetPath,
        String key,
        String actualValue,
        String expectedValue,
        String description,
        String rationale,
        String message) {
}