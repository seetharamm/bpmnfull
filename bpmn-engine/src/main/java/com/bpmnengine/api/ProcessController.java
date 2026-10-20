package com.bpmnengine.api;

import com.bpmnengine.engine.ProcessEngine;
import com.bpmnengine.engine.ProcessInstanceResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * REST API for process deployment and instance management.
 *
 * POST /api/deployments               — deploy a BPMN XML
 * POST /api/processes/{key}/start     — start a process instance
 * GET  /api/processes/{instanceId}    — get process instance state
 * POST /api/processes/{instanceId}/suspend  — suspend
 * POST /api/processes/{instanceId}/activate — activate
 * DELETE /api/processes/{instanceId}  — terminate
 */
@RestController
@RequestMapping("/api")
public class ProcessController {

    private final ProcessEngine engine;

    public ProcessController(ProcessEngine engine) {
        this.engine = engine;
    }

    // ── Deployment ────────────────────────────────────────────────────────────

    @PostMapping("/deployments")
    public ResponseEntity<DeploymentResponse> deploy(@RequestBody DeployRequest request) {
        String definitionId = engine.deployProcess(
                request.getResourceName() != null ? request.getResourceName() : "uploaded.bpmn",
                request.getBpmnXml()
        );
        return ResponseEntity.ok(new DeploymentResponse(definitionId));
    }

    // ── Process Instances ─────────────────────────────────────────────────────

    @PostMapping("/processes/{key}/start")
    public ResponseEntity<ProcessInstanceResult> startProcess(
            @PathVariable String key,
            @RequestBody(required = false) StartProcessRequest body) {
        Map<String, Object> vars = body != null ? body.getVariables() : Map.of();
        String businessKey = body != null ? body.getBusinessKey() : null;
        ProcessInstanceResult result = engine.startProcessByKey(key, businessKey, vars);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/processes")
    public ResponseEntity<List<ProcessInstanceResult>> listInstances(
            @RequestParam(required = false) String state) {
        return ResponseEntity.ok(engine.listInstances(state));
    }

    @GetMapping("/processes/{instanceId}")
    public ResponseEntity<ProcessInstanceResult> getInstance(@PathVariable String instanceId) {
        return ResponseEntity.ok(engine.getProcessInstance(instanceId));
    }

    @PostMapping("/processes/{instanceId}/suspend")
    public ResponseEntity<Void> suspend(@PathVariable String instanceId) {
        engine.suspendProcessInstance(instanceId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/processes/{instanceId}/activate")
    public ResponseEntity<Void> activate(@PathVariable String instanceId) {
        engine.activateProcessInstance(instanceId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/processes/{instanceId}")
    public ResponseEntity<Void> terminate(@PathVariable String instanceId,
                                           @RequestParam(defaultValue = "Terminated via API") String reason) {
        engine.terminateProcessInstance(instanceId, reason);
        return ResponseEntity.noContent().build();
    }

    // ── Messages & Signals ────────────────────────────────────────────────────

    @PostMapping("/messages/{messageName}/correlate")
    public ResponseEntity<Void> correlateMessage(
            @PathVariable String messageName,
            @RequestBody(required = false) CorrelateMessageRequest body) {
        String businessKey = body != null ? body.getBusinessKey() : null;
        Map<String, Object> vars = body != null ? body.getVariables() : Map.of();
        engine.correlateMessage(messageName, businessKey, vars);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/signals/{signalName}/broadcast")
    public ResponseEntity<Void> broadcastSignal(
            @PathVariable String signalName,
            @RequestBody(required = false) Map<String, Object> variables) {
        engine.signalAll(signalName, variables != null ? variables : Map.of());
        return ResponseEntity.noContent().build();
    }

    // ── Inner DTOs ────────────────────────────────────────────────────────────

    @Data public static class DeployRequest {
        private String resourceName;
        @NotBlank private String bpmnXml;
    }

    @Data public static class DeploymentResponse {
        private final String processDefinitionId;
    }

    @Data public static class StartProcessRequest {
        private String businessKey;
        private Map<String, Object> variables = Map.of();
    }

    @Data public static class CorrelateMessageRequest {
        private String businessKey;
        private Map<String, Object> variables = Map.of();
    }
}
