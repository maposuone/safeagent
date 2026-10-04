package com.oasis.safeagent.model;

public record ManualEditCandidate(
        int no,
        String type,
        String targetPath,
        String target,
        String key,
        String currentValue,
        String designValue,
        boolean editable,
        String reason) {
}
