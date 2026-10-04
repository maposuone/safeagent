package com.oasis.safeagent.controller;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.oasis.safeagent.model.ConfigDesignDocument;
import com.oasis.safeagent.model.ConfigDifference;
import com.oasis.safeagent.model.ManualEditCandidate;
import com.oasis.safeagent.model.RequestMode;
import com.oasis.safeagent.service.ConfigComparisonService;
import com.oasis.safeagent.service.ConfigDesignService;
import com.oasis.safeagent.service.ConfigManualEditService;
import com.oasis.safeagent.service.PromptInjectionScannerService;
import com.oasis.safeagent.service.RunHistoryService;
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
    private final RunHistoryService runHistoryService;
    private final ConfigDesignService configDesignService;
    private final ConfigComparisonService configComparisonService;
    private final ConfigManualEditService configManualEditService;

    public SafeAgentOrchestratorController(
            SafeAgentOrchestratorService orchestratorService,
            ToolExecutionService toolExecutionService,
            SecretScannerService secretScannerService,
            PromptInjectionScannerService promptInjectionScannerService,
            RunHistoryService runHistoryService,
            ConfigDesignService configDesignService,
            ConfigComparisonService configComparisonService,
            ConfigManualEditService configManualEditService) {

        this.orchestratorService = orchestratorService;
        this.toolExecutionService = toolExecutionService;
        this.secretScannerService = secretScannerService;
        this.promptInjectionScannerService = promptInjectionScannerService;
        this.runHistoryService = runHistoryService;
        this.configDesignService = configDesignService;
        this.configComparisonService = configComparisonService;
        this.configManualEditService = configManualEditService;
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
            @RequestPart("file") MultipartFile file,
            @RequestParam UUID runId,
            @RequestParam("targets") List<String> targets,
            @RequestParam("keys") List<String> keys,
            @RequestParam("currentValues") List<String> currentValues,
            @RequestParam("newValues") List<String> newValues,
            @RequestParam String approvalStatus) throws Exception {

        Path savedPath = toolExecutionService.saveFile(
                file.getOriginalFilename(),
                file.getBytes()
        );

        Map<String, Object> executionResult =
                orchestratorService.executeApprovedModifications(
                        savedPath,
                        targets,
                        keys,
                        currentValues,
                        newValues,
                        approvalStatus
                );

        Map<String, Object> result = new LinkedHashMap<>(executionResult);
        result.put("fileName", file.getOriginalFilename());

        /*
         * 選択した全変更がVERIFIEDまで完了した場合だけ、
         * 変更後の実ファイル内容をブラウザへ返す。
         */
        if ("COMPLETED".equals(result.get("status"))
                && "VERIFIED".equals(result.get("evidence"))) {

            result.put(
                    "downloadFileName",
                    file.getOriginalFilename()
            );

            result.put(
                    "modifiedFileContent",
                    toolExecutionService.readConfig(savedPath)
            );
        }

        runHistoryService.updateAfterApproval(
                runId,
                result
        );

        result.put("runId", runId.toString());

        return result;
    }


    @PostMapping("/manual-edit-execute")
    public Map<String, Object> manualEditExecute(
            @RequestPart("file") MultipartFile file,
            @RequestParam UUID runId,
            @RequestParam("targets") List<String> targets,
            @RequestParam("keys") List<String> keys,
            @RequestParam("currentValues") List<String> currentValues,
            @RequestParam("newValues") List<String> newValues) throws Exception {

        Path savedPath = toolExecutionService.saveFile(
                file.getOriginalFilename(),
                file.getBytes()
        );

        Map<String, Object> executionResult =
                orchestratorService.executeManualModifications(
                        savedPath,
                        targets,
                        keys,
                        currentValues,
                        newValues
                );

        Map<String, Object> result = new LinkedHashMap<>(executionResult);
        result.put("fileName", file.getOriginalFilename());
        result.put("manualEdit", true);

        if ("COMPLETED".equals(result.get("status"))
                && "VERIFIED".equals(result.get("evidence"))) {

            result.put("downloadFileName", file.getOriginalFilename());
            result.put(
                    "modifiedFileContent",
                    toolExecutionService.readConfig(savedPath)
            );
        }

        runHistoryService.updateAfterApproval(runId, result);
        result.put("runId", runId.toString());

        return result;
    }

    @PostMapping("/upload")
    public Map<String, Object> upload(
            @RequestPart("file") MultipartFile file) throws Exception {

        toolExecutionService.saveFile(
                file.getOriginalFilename(),
                file.getBytes()
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fileName", file.getOriginalFilename());
        result.put("status", "SAVED");

        return result;
    }

    @PostMapping("/run")
    public Map<String, Object> run(
            @RequestParam String requestMode,
            @RequestPart("file") MultipartFile file,
            @RequestPart(
                    value = "designFile",
                    required = false
            )
            MultipartFile designFile,
            @RequestParam(
                    value = "environment",
                    defaultValue = "PROD"
            )
            String environment) throws Exception {

        RequestMode mode =
                RequestMode.from(
                        requestMode
                );

        /*
         * ANALYZE / FIX は設計書比較が前提。
         * EXPLAIN は設定ファイルだけで実行できる。
         */
        if ((mode == RequestMode.ANALYZE
                || mode == RequestMode.FIX)
                && (designFile == null
                || designFile.isEmpty())) {

            throw new IllegalArgumentException(
                    "ANALYZE / FIXでは定数設計書を選択してください。"
            );
        }

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
         * Geminiへ送る前に必ずJava側で確認する。
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
            blocked.put(
                    "detectedSecrets",
                    detectedSecrets
            );
            blocked.put(
                    "aiAnalysis",
                    "NOT_EXECUTED"
            );
            blocked.put(
                    "fileName",
                    file.getOriginalFilename()
            );
            blocked.put(
                    "requestMode",
                    mode.name()
            );

            UUID runId =
                    runHistoryService.saveBlockedRun(
                            file.getOriginalFilename(),
                            mode.name(),
                            "SECRET_DETECTED"
                    );

            blocked.put(
                    "runId",
                    runId.toString()
            );

            return blocked;
        }

        /*
         * 2. Prompt Injection scan
         */
        List<String> detectedPromptInjection =
                promptInjectionScannerService.scan(
                        fileContent
                );

        if (!detectedPromptInjection.isEmpty()) {

            Map<String, Object> blocked =
                    new LinkedHashMap<>();

            blocked.put("status", "BLOCKED");
            blocked.put(
                    "reason",
                    "PROMPT_INJECTION_DETECTED"
            );
            blocked.put(
                    "detectedPatterns",
                    detectedPromptInjection
            );
            blocked.put(
                    "aiAnalysis",
                    "NOT_EXECUTED"
            );
            blocked.put(
                    "fileName",
                    file.getOriginalFilename()
            );
            blocked.put(
                    "requestMode",
                    mode.name()
            );

            UUID runId =
                    runHistoryService.saveBlockedRun(
                            file.getOriginalFilename(),
                            mode.name(),
                            "PROMPT_INJECTION_DETECTED"
                    );

            blocked.put(
                    "runId",
                    runId.toString()
            );

            return blocked;
        }

        List<ConfigDifference> designDifferences =
                List.of();

        List<ManualEditCandidate> manualEditCandidates =
                List.of();

        ConfigDesignDocument design =
                null;

        /*
         * 3. ANALYZE / FIX の場合だけ
         *    定数設計書を読み込み、設定ファイルと比較する。
         */
        if (mode == RequestMode.ANALYZE
                || mode == RequestMode.FIX) {

            design =
                    configDesignService
                            .loadForTarget(
                                    designFile,
                                    file.getOriginalFilename()
                            );

            designDifferences =
                    configComparisonService.compare(
                            file.getOriginalFilename(),
                            fileContent,
                            design,
                            environment
                    );

            /*
             * 手動修正候補はFIXだけ。
             * ANALYZEは「見る・分析する」だけなので変更候補を実行可能にしない。
             */
            if (mode == RequestMode.FIX) {

                manualEditCandidates =
                        configManualEditService
                                .createCandidates(
                                        fileContent,
                                        design,
                                        environment
                                );
            }
        }

        /*
         * 4. Geminiへの依頼文はユーザー自由入力ではなく、
         *    選択された3モードからJava側で固定生成する。
         */
        String internalRequest =
                mode.internalRequest();

        Map<String, Object> result =
                orchestratorService.runInitialFlow(
                        internalRequest,
                        fileContent
                );

        /*
         * EXPLAIN / ANALYZEでは絶対に承認・変更へ進ませない。
         * Geminiが変更案を返しても、Java側で変更候補を無効化する。
         */
        if (mode != RequestMode.FIX) {

            result.remove("proposedKey");
            result.remove("proposedValue");
            result.remove("proposedChanges");

            result.put(
                    "status",
                    "COMPLETED"
            );

            result.put(
                    "decision",
                    mode == RequestMode.EXPLAIN
                            ? "EXPLANATION_ONLY"
                            : "ANALYSIS_ONLY"
            );
        }

        result.put(
                "fileName",
                file.getOriginalFilename()
        );

        result.put(
                "requestMode",
                mode.name()
        );

        result.put(
                "environment",
                environment
        );

        result.put(
                "allowApproval",
                mode == RequestMode.FIX
        );

        result.put(
                "allowManualEdit",
                mode == RequestMode.FIX
        );

        if (design != null) {

            result.put(
                    "designFileName",
                    designFile.getOriginalFilename()
            );

            result.put(
                    "designTargetFileName",
                    design.fileName()
            );

            result.put(
                    "designDifferenceCount",
                    designDifferences.size()
            );

            result.put(
                    "designDifferences",
                    designDifferences
            );

            result.put(
                    "designComparisonStatus",
                    designDifferences.isEmpty()
                            ? "MATCH"
                            : "MISMATCH"
            );

        } else {

            result.put(
                    "designDifferenceCount",
                    0
            );

            result.put(
                    "designDifferences",
                    List.of()
            );

            result.put(
                    "designComparisonStatus",
                    "NOT_APPLICABLE"
            );
        }

        result.put(
                "manualEditCandidates",
                manualEditCandidates
        );

        result.put(
                "manualEditCandidateCount",
                manualEditCandidates.size()
        );

        UUID runId =
                runHistoryService.saveInitialRun(
                        file.getOriginalFilename(),
                        mode.name(),
                        result
                );

        result.put(
                "runId",
                runId.toString()
        );

        return result;
    }

}
