package com.oasis.safeagent.controller;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.oasis.safeagent.service.PromptInjectionScannerService;
import com.oasis.safeagent.service.SafeAgentOrchestratorService;
import com.oasis.safeagent.service.SecretScannerService;
import com.oasis.safeagent.service.ToolExecutionService;

@RestController
@RequestMapping("/api/safeagent")
public class SafeAgentOrchestratorController {

    private final SafeAgentOrchestratorService orchestratorService;
    private final ToolExecutionService toolExecutionService;
    private final SecretScannerService secretScannerService;
    private final PromptInjectionScannerService promptInjectionScannerService;

    public SafeAgentOrchestratorController(
            SafeAgentOrchestratorService orchestratorService,
            ToolExecutionService toolExecutionService,
            SecretScannerService secretScannerService,
            PromptInjectionScannerService promptInjectionScannerService) {

        this.orchestratorService = orchestratorService;
        this.toolExecutionService = toolExecutionService;
        this.secretScannerService = secretScannerService;
        this.promptInjectionScannerService = promptInjectionScannerService;
    }

    @PostMapping("/decide")
    public Map<String, Object> decide(
            @RequestParam String request,
            @RequestParam String content,
            @RequestParam(required = false) String analysisResult) {

        return orchestratorService.decideAndEvaluate(
                request,
                content,
                analysisResult
        );
    }

    @PostMapping("/approve-and-execute")
    public Map<String, Object> approveAndExecute(
            @RequestParam String filePath,
            @RequestParam String key,
            @RequestParam String newValue,
            @RequestParam String approvalStatus) throws Exception {

        return orchestratorService.executeApprovedModification(
                Path.of(filePath),
                key,
                newValue,
                approvalStatus
        );
    }

    @PostMapping("/upload")
    public Map<String, Object> upload(
            @RequestPart("file") MultipartFile file) throws Exception {

        Path savedPath =
                toolExecutionService.saveFile(
                        file.getOriginalFilename(),
                        file.getBytes()
                );

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put("fileName", file.getOriginalFilename());
        result.put("savedPath", savedPath.toString());
        result.put("status", "SAVED");

        return result;
    }

    @PostMapping("/run")
    public Map<String, Object> run(
            @RequestParam String request,
            @RequestPart("file") MultipartFile file) throws Exception {

        Path savedPath =
                toolExecutionService.saveFile(
                        file.getOriginalFilename(),
                        file.getBytes()
                );

        String fileContent =
                toolExecutionService.readConfig(
                        savedPath
                );

        /*
         * 1. Secret scan
         * Geminiへ送る前に確認する
         */
        List<String> detectedSecrets =
                secretScannerService.scan(
                        fileContent
                );

        if (!detectedSecrets.isEmpty()) {

            Map<String, Object> blocked =
                    new LinkedHashMap<>();

            blocked.put("status", "BLOCKED");
            blocked.put("reason", "SECRET_DETECTED");
            blocked.put("detectedSecrets", detectedSecrets);
            blocked.put("aiAnalysis", "NOT_EXECUTED");
            blocked.put("fileName", file.getOriginalFilename());

            return blocked;
        }

        /*
         * 2. Prompt Injection scan
         * これもGeminiより前に確認する
         */
        List<String> detectedPromptInjection =
                promptInjectionScannerService.scan(
                        fileContent
                );

        if (!detectedPromptInjection.isEmpty()) {

            Map<String, Object> blocked =
                    new LinkedHashMap<>();

            blocked.put("status", "BLOCKED");
            blocked.put("reason", "PROMPT_INJECTION_DETECTED");
            blocked.put("detectedPatterns", detectedPromptInjection);
            blocked.put("aiAnalysis", "NOT_EXECUTED");
            blocked.put("fileName", file.getOriginalFilename());

            return blocked;
        }

        /*
         * 3. 安全なファイルだけGeminiへ渡す
         */
        Map<String, Object> result =
                orchestratorService.runInitialFlow(
                        request,
                        fileContent
                );

        result.put(
                "fileName",
                file.getOriginalFilename()
        );

        result.put(
                "savedPath",
                savedPath.toString()
        );

        return result;
    }
}