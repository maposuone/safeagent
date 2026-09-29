package com.oasis.safeagent.service;

import org.springframework.stereotype.Service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;

@Service
public class GeminiService {

    public String analyze(String prompt) {

        try (Client client = Client.builder()
        		.project("safeagent-2026")
                .location("global")
                .vertexAI(true)
                .httpOptions(
                    HttpOptions.builder()
                        .apiVersion("v1")
                        .build())
                .build()) {

            GenerateContentResponse response =
                    client.models.generateContent(
                            "gemini-2.5-flash",
                            prompt,
                            null);

            return response.text();
        }
    }
}