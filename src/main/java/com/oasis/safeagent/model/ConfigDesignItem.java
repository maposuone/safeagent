package com.oasis.safeagent.model;

public record ConfigDesignItem(
        int no,
        String format,
        String targetPath,
        String key,
        String productionValue,
        String testValue,
        String defaultValue,
        String description,
        String rationale,
        String note,
        String required,
        String impact,
        String classification) {

    public String expectedValue(String environment) {

        if ("TEST".equalsIgnoreCase(environment)
                || "テスト".equals(environment)) {

            return testValue;
        }

        return productionValue;
    }
}