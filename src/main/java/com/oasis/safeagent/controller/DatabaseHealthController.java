package com.oasis.safeagent.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oasis.safeagent.service.DatabaseHealthService;

@RestController
@RequestMapping("/api/database")
public class DatabaseHealthController {

    private final DatabaseHealthService databaseHealthService;

    public DatabaseHealthController(
            DatabaseHealthService databaseHealthService) {

        this.databaseHealthService = databaseHealthService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {

        Map<String, Object> result =
                new LinkedHashMap<>();

        String database =
                databaseHealthService.checkConnection();

        result.put("status", "UP");
        result.put("database", database);

        return result;
    }
}