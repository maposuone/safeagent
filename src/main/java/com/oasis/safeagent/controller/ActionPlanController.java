package com.oasis.safeagent.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oasis.safeagent.service.ActionPlanService;

@RestController
@RequestMapping("/api/agent")
public class ActionPlanController {

    private final ActionPlanService actionPlanService;

    public ActionPlanController(ActionPlanService actionPlanService) {
        this.actionPlanService = actionPlanService;
    }

    @PostMapping("/plan")
    public String createPlan(
            @RequestParam String request,
            @RequestParam String content) {

        return actionPlanService.createPlan(request, content);
    }

    @PostMapping("/evaluate")
    public String evaluateAction(
            @RequestParam String action) {

        return actionPlanService.evaluateAction(action);
    }
}