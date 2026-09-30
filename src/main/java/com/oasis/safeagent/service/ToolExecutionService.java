package com.oasis.safeagent.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ToolExecutionService {

    private final Path workDirectory;

    public ToolExecutionService() {
        this.workDirectory = Paths.get(
                System.getProperty("java.io.tmpdir"),
                "safeagent"
        ).toAbsolutePath().normalize();
    }

    /*
     * 今までのAgentExecutionServiceとの互換用。
     *
     * ファイルを指定しない状態では、
     * 本物のToolを実行させない。
     */
    public String execute(String action) {

        if (action == null || action.isBlank()) {
            return "EXECUTION_REJECTED";
        }

        return switch (action.toUpperCase()) {
            case "READ_CONFIG",
                 "ANALYZE_CONFIG",
                 "COMPARE_CONFIG",
                 "MODIFY_CONFIG" ->
                    "EXECUTION_REQUIRES_FILE";

            default ->
                    "EXECUTION_BLOCKED";
        };
    }

    /*
     * アップロードされた設定ファイルを
     * SafeAgent専用領域に保存する。
     */
    public Path saveFile(
            String originalFileName,
            byte[] content) throws IOException {

        Files.createDirectories(workDirectory);

        String safeFileName = sanitizeFileName(originalFileName);

        Path target = workDirectory
                .resolve(System.currentTimeMillis() + "-" + safeFileName)
                .normalize();

        validateWorkspacePath(target);

        Files.write(
                target,
                content,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );

        return target;
    }

    /*
     * 実際に設定ファイルを読むTool
     */
    public String readConfig(Path filePath) throws IOException {

        Path safePath = validateWorkspacePath(filePath);

        if (!Files.exists(safePath)) {
            return "READ_FAILED: FILE_NOT_FOUND";
        }

        return Files.readString(
                safePath,
                StandardCharsets.UTF_8
        );
    }

    /*
     * 実際に properties の値を書き換えるTool
     *
     * 例:
     * logging.level.root=DEBUG
     *
     * ↓
     *
     * logging.level.root=INFO
     */
    public String modifyConfig(
            Path filePath,
            String key,
            String newValue) throws IOException {

        Path safePath = validateWorkspacePath(filePath);

        if (!Files.exists(safePath)) {
            return "MODIFY_FAILED: FILE_NOT_FOUND";
        }

        if (key == null || key.isBlank()) {
            return "MODIFY_FAILED: INVALID_KEY";
        }

        if (newValue == null) {
            return "MODIFY_FAILED: INVALID_VALUE";
        }

        List<String> lines = Files.readAllLines(
                safePath,
                StandardCharsets.UTF_8
        );

        int matchCount = 0;
        int targetIndex = -1;

        for (int i = 0; i < lines.size(); i++) {

            String line = lines.get(i);
            String trimmed = line.trim();

            if (trimmed.isEmpty()
                    || trimmed.startsWith("#")
                    || trimmed.startsWith("!")) {
                continue;
            }

            int equalsIndex = trimmed.indexOf('=');

            if (equalsIndex < 0) {
                continue;
            }

            String existingKey =
                    trimmed.substring(0, equalsIndex).trim();

            if (key.equals(existingKey)) {
                matchCount++;
                targetIndex = i;
            }
        }

        if (matchCount == 0) {
            return "MODIFY_FAILED: KEY_NOT_FOUND";
        }

        /*
         * 同じキーが複数ある場合は
         * SafeAgentが勝手に判断して変更しない。
         */
        if (matchCount > 1) {
            return "MODIFY_FAILED: DUPLICATE_KEY";
        }

        /*
         * 変更前にバックアップを作る。
         */
        Path backupPath = safePath.resolveSibling(
                safePath.getFileName().toString() + ".bak"
        );

        Files.copy(
                safePath,
                backupPath,
                StandardCopyOption.REPLACE_EXISTING
        );

        List<String> updatedLines = new ArrayList<>(lines);

        updatedLines.set(
                targetIndex,
                key + "=" + newValue
        );

        Files.write(
                safePath,
                updatedLines,
                StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );

        return "MODIFIED: " + key + "=" + newValue;
    }

    /*
     * Evidence Validatorから再確認するときにも使える。
     */
    public boolean verifyValue(
            Path filePath,
            String key,
            String expectedValue) throws IOException {

        String content = readConfig(filePath);

        String expected =
                key + "=" + expectedValue;

        return content.lines()
                .map(String::trim)
                .anyMatch(expected::equals);
    }

    private Path validateWorkspacePath(Path path) {

        Path normalized =
                path.toAbsolutePath().normalize();

        if (!normalized.startsWith(workDirectory)) {
            throw new SecurityException(
                    "Access outside SafeAgent workspace is blocked."
            );
        }

        return normalized;
    }

    private String sanitizeFileName(String originalFileName) {

        if (originalFileName == null
                || originalFileName.isBlank()) {
            return "config.properties";
        }

        String normalized =
                originalFileName.replace("\\", "/");

        int lastSlash =
                normalized.lastIndexOf('/');

        if (lastSlash >= 0) {
            normalized =
                    normalized.substring(lastSlash + 1);
        }

        normalized =
                normalized.replaceAll(
                        "[^a-zA-Z0-9._-]",
                        "_"
                );

        if (normalized.isBlank()) {
            return "config.properties";
        }

        return normalized;
    }
}