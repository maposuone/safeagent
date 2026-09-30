package com.oasis.safeagent.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oasis.safeagent.service.AuditService;

@RestController
@RequestMapping("/api/safeagent")
public class AuditController {

    private final AuditService auditService;

    public AuditController(
            AuditService auditService) {

        this.auditService = auditService;
    }

    @GetMapping("/audit")
    public Map<String, Object> getLatestAudit() {

        return auditService.getLatestAudit();
    }
}