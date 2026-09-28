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

import com.oasis.safeagent.service.SecretScannerService;

@RestController
@RequestMapping("/api/files")
public class FileAnalysisController {

    private final SecretScannerService secretScannerService;

    public FileAnalysisController(SecretScannerService secretScannerService) {
        this.secretScannerService = secretScannerService;
    }

    @PostMapping("/analyze")
    public Map<String, Object> analyzeFile(
            @RequestParam("file") MultipartFile file) throws IOException {

        String content = new String(
                file.getBytes(),
                StandardCharsets.UTF_8
        );

        List<String> detectedSecrets =
                secretScannerService.scan(content);

        String status =
                detectedSecrets.isEmpty() ? "SAFE" : "BLOCKED";

        return Map.of(
                "fileName", file.getOriginalFilename(),
                "size", file.getSize(),
                "status", status,
                "detectedSecrets", detectedSecrets
        );
    }
}