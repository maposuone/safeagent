package com.oasis.safeagent.model;

import java.util.List;

public record ConfigDesignDocument(
        String fileName,
        List<ConfigDesignItem> items) {
}