package com.oasis.safeagent.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.oasis.safeagent.service.PermissionCheckService;
import com.oasis.safeagent.service.PromptInjectionScannerService;
import com.oasis.safeagent.service.SecretScannerService;
import com.oasis.safeagent.service.RiskAssessmentService;

@RestController
@RequestMapping("/api/files")
public class FileAnalysisController {

    private final SecretScannerService secretScannerService;
    private final PromptInjectionScannerService promptInjectionScannerService;
    private final PermissionCheckService permissionCheckService;
    private final RiskAssessmentService riskAssessmentService;   

    public FileAnalysisController(
            SecretScannerService secretScannerService,
            PromptInjectionScannerService promptInjectionScannerService,
            PermissionCheckService permissionCheckService,
            RiskAssessmentService riskAssessmentService) {

        this.secretScannerService = secretScannerService;
        this.promptInjectionScannerService = promptInjectionScannerService;
        this.permissionCheckService = permissionCheckService;
        this.riskAssessmentService = riskAssessmentService;
    }

    @PostMapping("/analyze")
    public Map<String, Object> analyzeFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "ANALYZE_CONFIG") String action)
            throws IOException {

        String content = new String(
                file.getBytes(),
                StandardCharsets.UTF_8
        );

        List<String> detectedSecrets =
                secretScannerService.scan(content);

        List<String> detectedPromptInjections =
                promptInjectionScannerService.scan(content);

        boolean permissionAllowed =
                permissionCheckService.isAllowed(action);

        String riskLevel =
                riskAssessmentService.assess(action);

        boolean humanApprovalRequired =
                "HIGH".equals(riskLevel) || "CRITICAL".equals(riskLevel);

        String status =
                detectedSecrets.isEmpty()
                && detectedPromptInjections.isEmpty()
                && permissionAllowed
                        ? "SAFE"
                        : "BLOCKED";

        return Map.of(
                "fileName", file.getOriginalFilename(),
                "size", file.getSize(),
                "action", action,
                "permissionAllowed", permissionAllowed,
                "riskLevel", riskLevel,
                "humanApprovalRequired", humanApprovalRequired,
                "status", status,
                "detectedSecrets", detectedSecrets,
                "detectedPromptInjections", detectedPromptInjections
        );
    }
}