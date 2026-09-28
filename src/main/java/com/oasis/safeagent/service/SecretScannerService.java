package com.oasis.safeagent.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class SecretScannerService {

    public List<String> scan(String content) {

        List<String> detectedSecrets = new ArrayList<>();

        String[] lines = content.split("\\R");

        for (String line : lines) {

            String lowerLine = line.toLowerCase();

            if (lowerLine.contains("password")) {
                detectedSecrets.add("PASSWORD");
            }

            if (lowerLine.contains("api.key")
                    || lowerLine.contains("apikey")
                    || lowerLine.contains("api_key")) {
                detectedSecrets.add("API_KEY");
            }

            if (lowerLine.contains("secret")) {
                detectedSecrets.add("SECRET");
            }

            if (lowerLine.contains("token")) {
                detectedSecrets.add("TOKEN");
            }
        }

        return detectedSecrets;
    }
}