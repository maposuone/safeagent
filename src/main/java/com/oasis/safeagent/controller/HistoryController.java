package com.oasis.safeagent.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oasis.safeagent.service.RunHistoryService;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private static final int PAGE_SIZE = 10;

    private final RunHistoryService runHistoryService;

    public HistoryController(
            RunHistoryService runHistoryService) {

        this.runHistoryService = runHistoryService;
    }

    @GetMapping
    public Map<String, Object> history(
            @RequestParam(defaultValue = "0") int page) {

        if (page < 0) {
            page = 0;
        }

        long total =
                runHistoryService.countHistory();

        int totalPages =
                total == 0
                        ? 1
                        : (int) Math.ceil(
                                (double) total / PAGE_SIZE
                        );

        if (page >= totalPages) {
            page = totalPages - 1;
        }

        List<Map<String, Object>> items =
                runHistoryService.findHistory(
                        page,
                        PAGE_SIZE
                );

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put("items", items);
        result.put("page", page);
        result.put("size", PAGE_SIZE);
        result.put("totalElements", total);
        result.put("totalPages", totalPages);

        return result;
    }
}