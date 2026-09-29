package com.oasis.safeagent.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oasis.safeagent.service.ApprovalService;

@RestController
@RequestMapping("/api/approval")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @PostMapping("/approve")
    public String approve(@RequestParam String action) {
        return approvalService.approve(action);
    }

    @PostMapping("/reject")
    public String reject(@RequestParam String action) {
        return approvalService.reject(action);
    }
}