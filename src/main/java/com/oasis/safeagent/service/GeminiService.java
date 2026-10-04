package com.oasis.safeagent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;

@Service
public class GeminiService {

    private static final Logger log =
            LoggerFactory.getLogger(GeminiService.class);

    public String analyze(String prompt) {

        log.info("Gemini API call start");

        try (Client client = Client.builder()
                .project("safeagent-2026")
                .location("global")
                .vertexAI(true)
                .httpOptions(
                        HttpOptions.builder()
                                .apiVersion("v1")
                                .build())
                .build()) {

            log.info("Gemini client created");

            GenerateContentResponse response =
                    client.models.generateContent(
                            "gemini-2.5-flash",
                            prompt,
                            null);

            log.info("Gemini API call success");

            return response.text();

        } catch (Exception e) {

            log.error(
                    "Gemini API call failed: type={}, message={}",
                    e.getClass().getName(),
                    e.getMessage(),
                    e
            );

            throw e;
        }
    }
}