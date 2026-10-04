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
    @Test
    void shouldModifyApacheStyleDirective() throws Exception {

        ToolExecutionService service =
                new ToolExecutionService();

        String originalContent =
                "Listen 80\n"
                + "User nobody\n"
                + "Group nobody\n";

        Path filePath = service.saveFile(
                "httpd.conf",
                originalContent.getBytes(StandardCharsets.UTF_8)
        );

        String result = service.modifyConfig(
                filePath,
                "User",
                "apache"
        );

        assertEquals(
                "MODIFIED: User apache",
                result
        );

        String after = service.readConfig(filePath);

        assertTrue(after.contains("User apache"));
        assertTrue(!after.contains("User=apache"));

        assertTrue(
                service.verifyValue(
                        filePath,
                        "User",
                        "apache"
                )
        );
    }

    @Test
    void shouldModifyXmlAttributeAndVerifyEvidence() throws Exception {

        ToolExecutionService service =
                new ToolExecutionService();

        String originalContent =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<Server>\n"
                + "  <Service>\n"
                + "    <Engine>\n"
                + "      <Host name=\"localhost\" autoDeploy=\"true\"/>\n"
                + "    </Engine>\n"
                + "  </Service>\n"
                + "</Server>\n";

        Path filePath = service.saveFile(
                "server.xml",
                originalContent.getBytes(StandardCharsets.UTF_8)
        );

        String selector =
                "XML:/Server/Service/Engine/Host@autoDeploy";

        assertTrue(
                service.isUniqueEditableTarget(
                        originalContent,
                        selector
                )
        );

        String result = service.modifyConfig(
                filePath,
                selector,
                "false"
        );

        assertEquals(
                "MODIFIED: XML:/Server/Service/Engine/Host@autoDeploy=false",
                result
        );

        String after = service.readConfig(filePath);

        assertTrue(after.contains("autoDeploy=\"false\""));

        assertTrue(
                service.verifyValue(
                        filePath,
                        selector,
                        "false"
                )
        );

        Path backupPath =
                filePath.resolveSibling(
                        filePath.getFileName().toString() + ".bak"
                );

        assertTrue(Files.exists(backupPath));
        String backup = Files.readString(
                backupPath,
                StandardCharsets.UTF_8
        );
        assertTrue(backup.contains("autoDeploy=\"true\""));
    }

    @Test
    void shouldRejectNonUniqueXmlTarget() {

        ToolExecutionService service =
                new ToolExecutionService();

        String xml =
                "<Server><Service><Engine>"
                + "<Host autoDeploy=\"true\"/>"
                + "<Host autoDeploy=\"true\"/>"
                + "</Engine></Service></Server>";

        assertTrue(
                !service.isUniqueEditableTarget(
                        xml,
                        "XML:/Server/Service/Engine/Host@autoDeploy"
                )
        );
    }

}