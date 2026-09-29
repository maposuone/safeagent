package com.oasis.safeagent.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class PromptInjectionScannerService {

    public List<String> scan(String content) {

        List<String> detectedPatterns = new ArrayList<>();

        String lowerContent = content.toLowerCase();

        if (lowerContent.contains("ignore all previous instructions")) {
            detectedPatterns.add("IGNORE_PREVIOUS_INSTRUCTIONS");
        }

        if (lowerContent.contains("ignore previous instructions")) {
            detectedPatterns.add("IGNORE_PREVIOUS_INSTRUCTIONS");
        }

        if (lowerContent.contains("send all environment variables")) {
            detectedPatterns.add("ENVIRONMENT_VARIABLE_EXFILTRATION");
        }

        if (lowerContent.contains("reveal system prompt")) {
            detectedPatterns.add("SYSTEM_PROMPT_EXTRACTION");
        }

        return detectedPatterns;
    }
}