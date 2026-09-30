package com.oasis.safeagent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ToolExecutionServiceTest {

    @Test
    void shouldActuallyModifyConfigurationFile() throws Exception {

        ToolExecutionService service =
                new ToolExecutionService();

        String originalContent =
                "server.port=8080\n"
                + "logging.level.root=DEBUG\n";

        Path filePath = service.saveFile(
                "demo.properties",
                originalContent.getBytes(StandardCharsets.UTF_8)
        );

        // ① 本当に保存されたか
        assertTrue(Files.exists(filePath));

        // ② 変更前はDEBUG
        String before =
                service.readConfig(filePath);

        assertTrue(
                before.contains(
                        "logging.level.root=DEBUG"
                )
        );

        // ③ 実際にINFOへ変更
        String result =
                service.modifyConfig(
                        filePath,
                        "logging.level.root",
                        "INFO"
                );

        assertEquals(
                "MODIFIED: logging.level.root=INFO",
                result
        );

        // ④ 実ファイルを再読込して確認
        String after =
                service.readConfig(filePath);

        assertTrue(
                after.contains(
                        "logging.level.root=INFO"
                )
        );

        // ⑤ Evidence用検証
        assertTrue(
                service.verifyValue(
                        filePath,
                        "logging.level.root",
                        "INFO"
                )
        );

        // ⑥ バックアップも本当に作られたか
        Path backupPath =
                filePath.resolveSibling(
                        filePath.getFileName().toString()
                        + ".bak"
                );

        assertTrue(Files.exists(backupPath));

        String backup =
                Files.readString(
                        backupPath,
                        StandardCharsets.UTF_8
                );

        assertTrue(
                backup.contains(
                        "logging.level.root=DEBUG"
                )
        );
    }
}