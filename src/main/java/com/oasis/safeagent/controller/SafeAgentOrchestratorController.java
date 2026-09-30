package com.oasis.safeagent.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import java.nio.file.Path;

import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oasis.safeagent.service.ToolExecutionService;
import com.oasis.safeagent.service.SafeAgentOrchestratorService;

@RestController
@RequestMapping("/api/safeagent")
public class SafeAgentOrchestratorController {

	private final SafeAgentOrchestratorService orchestratorService;
	private final ToolExecutionService toolExecutionService;
	
	public SafeAgentOrchestratorController(
	        SafeAgentOrchestratorService orchestratorService,
	        ToolExecutionService toolExecutionService) {

	    this.orchestratorService = orchestratorService;
	    this.toolExecutionService = toolExecutionService;
	}

    @PostMapping("/decide")
    public Map<String, Object> decide(
            @RequestParam String request,
            @RequestParam String content,
            @RequestParam(required = false) String analysisResult) {

        return orchestratorService.decideAndEvaluate(
                request,
                content,
                analysisResult
        );
    }
    
    @PostMapping("/approve-and-execute")
    public Map<String, Object> approveAndExecute(
            @RequestParam String filePath,
            @RequestParam String key,
            @RequestParam String newValue,
            @RequestParam String approvalStatus) throws Exception {

        return orchestratorService.executeApprovedModification(
                java.nio.file.Path.of(filePath),
                key,
                newValue,
                approvalStatus
        );
    }
    
    @PostMapping("/upload")
    public Map<String, Object> upload(
            @RequestPart("file") MultipartFile file) throws Exception {

        Path savedPath =
                toolExecutionService.saveFile(
                        file.getOriginalFilename(),
                        file.getBytes()
                );

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put("fileName", file.getOriginalFilename());
        result.put("savedPath", savedPath.toString());
        result.put("status", "SAVED");

        return result;
    }
    
    @PostMapping("/run")
    public Map<String, Object> run(
            @RequestParam String request,
            @RequestPart("file") MultipartFile file) throws Exception {

        Path savedPath =
                toolExecutionService.saveFile(
                        file.getOriginalFilename(),
                        file.getBytes()
                );

        String fileContent =
                toolExecutionService.readConfig(
                        savedPath
                );

        Map<String, Object> result =
                orchestratorService.runInitialFlow(
                        request,
                        fileContent
                );

        result.put(
                "fileName",
                file.getOriginalFilename()
        );

        result.put(
                "savedPath",
                savedPath.toString()
        );

        return result;
    }
}