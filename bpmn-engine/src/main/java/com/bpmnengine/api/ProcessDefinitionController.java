package com.bpmnengine.api;

import com.bpmnengine.api.dto.*;
import com.bpmnengine.engine.ProcessEngine;
import com.bpmnengine.engine.ProcessInstanceResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for process definition management and versioning.
 *
 * GET    /api/definitions                                   — list all active definitions
 * GET    /api/definitions?q=search                         — search definitions
 * GET    /api/definitions/by-id/{id}                       — get definition by ID
 * GET    /api/definitions/{key}                            — get active version (includes XML)
 * GET    /api/definitions/{key}/versions                   — version history
 * GET    /api/definitions/{key}/versions/{version}         — specific version (includes XML)
 * GET    /api/definitions/{key}/versions/{version}/bpmn    — download raw BPMN XML
 * POST   /api/definitions/{key}/versions/{version}/activate — set active version
 * DELETE /api/definitions/{key}/versions/{version}         — delete a version
 * DELETE /api/definitions/{key}                            — delete all versions
 * POST   /api/definitions/{key}/suspend                    — suspend all versions
 * POST   /api/definitions/{key}/activate                   — activate all versions
 * POST   /api/definitions/{key}/versions/{version}/suspend  — suspend one version
 * POST   /api/definitions/{key}/versions/{version}/resume   — resume one version
 * POST   /api/definitions/{key}/versions/{version}/start    — start specific version
 * POST   /api/definitions/deploy                            — rich deploy (with metadata)
 */
@RestController
@RequestMapping("/api/definitions")
public class ProcessDefinitionController {

    private final ProcessEngine engine;

    public ProcessDefinitionController(ProcessEngine engine) {
        this.engine = engine;
    }

    // ── Deploy ────────────────────────────────────────────────────────────────

    @PostMapping("/deploy")
    public ResponseEntity<ProcessDefinitionDto> deploy(
            @Valid @RequestBody DeployProcessRequest request) {
        ProcessDefinitionDto dto = engine.deployProcess(request);
        return ResponseEntity.ok(dto);
    }

    // ── List / search ─────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<ProcessDefinitionDto>> list(
            @RequestParam(required = false) String q) {
        List<ProcessDefinitionDto> result = (q != null && !q.isBlank())
                ? engine.searchDefinitions(q)
                : engine.listDefinitions();
        return ResponseEntity.ok(result);
    }

    // ── Get by ID ─────────────────────────────────────────────────────────────

    @GetMapping("/by-id/{id}")
    public ResponseEntity<ProcessDefinitionDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(engine.getDefinitionById(id));
    }

    // ── Active version ────────────────────────────────────────────────────────

    @GetMapping("/{key}")
    public ResponseEntity<ProcessDefinitionDto> getActive(@PathVariable String key) {
        return ResponseEntity.ok(engine.getActiveDefinition(key));
    }

    // ── Version history ───────────────────────────────────────────────────────

    @GetMapping("/{key}/versions")
    public ResponseEntity<ProcessVersionSummaryDto> getVersionHistory(@PathVariable String key) {
        return ResponseEntity.ok(engine.getVersionHistory(key));
    }

    // ── Specific version ──────────────────────────────────────────────────────

    @GetMapping("/{key}/versions/{version}")
    public ResponseEntity<ProcessDefinitionDto> getVersion(
            @PathVariable String key,
            @PathVariable int version) {
        return ResponseEntity.ok(engine.getDefinitionByVersion(key, version));
    }

    // ── Download raw BPMN XML ─────────────────────────────────────────────────

    @GetMapping("/{key}/versions/{version}/bpmn")
    public ResponseEntity<String> getBpmnXml(
            @PathVariable String key,
            @PathVariable int version) {
        String xml = engine.getBpmnXml(key, version);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + key + "-v" + version + ".bpmn20.xml\"")
                .body(xml);
    }

    // ── Set active version ────────────────────────────────────────────────────

    @PostMapping("/{key}/versions/{version}/activate")
    public ResponseEntity<ProcessDefinitionDto> setActive(
            @PathVariable String key,
            @PathVariable int version) {
        return ResponseEntity.ok(engine.setActiveVersion(key, version));
    }

    // ── Delete version ────────────────────────────────────────────────────────

    @DeleteMapping("/{key}/versions/{version}")
    public ResponseEntity<Void> deleteVersion(
            @PathVariable String key,
            @PathVariable int version) {
        engine.deleteVersion(key, version);
        return ResponseEntity.noContent().build();
    }

    // ── Delete all versions ───────────────────────────────────────────────────

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> deleteAll(@PathVariable String key) {
        engine.deleteAllVersions(key);
        return ResponseEntity.noContent().build();
    }

    // ── Suspend / activate all versions ──────────────────────────────────────

    @PostMapping("/{key}/suspend")
    public ResponseEntity<Void> suspendAll(@PathVariable String key) {
        engine.suspendProcessDefinition(key);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{key}/activate")
    public ResponseEntity<Void> activateAll(@PathVariable String key) {
        engine.activateProcessDefinition(key);
        return ResponseEntity.noContent().build();
    }

    // ── Suspend / activate one version ────────────────────────────────────────

    @PostMapping("/{key}/versions/{version}/suspend")
    public ResponseEntity<Void> suspendVersion(
            @PathVariable String key,
            @PathVariable int version) {
        engine.suspendProcessDefinitionVersion(key, version);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{key}/versions/{version}/resume")
    public ResponseEntity<Void> resumeVersion(
            @PathVariable String key,
            @PathVariable int version) {
        engine.activateProcessDefinitionVersion(key, version);
        return ResponseEntity.noContent().build();
    }

    // ── Start a specific version ──────────────────────────────────────────────

    @PostMapping("/{key}/versions/{version}/start")
    public ResponseEntity<ProcessInstanceResult> startVersion(
            @PathVariable String key,
            @PathVariable int version,
            @RequestBody(required = false) StartVersionRequest body) {
        String businessKey = body != null ? body.getBusinessKey() : null;
        Map<String, Object> vars = body != null && body.getVariables() != null
                ? body.getVariables() : Map.of();
        ProcessInstanceResult result = engine.startProcessByKeyAndVersion(key, version, businessKey, vars);
        return ResponseEntity.ok(result);
    }

    // ── Inner DTOs ────────────────────────────────────────────────────────────

    @lombok.Data
    public static class StartVersionRequest {
        private String businessKey;
        private Map<String, Object> variables;
    }
}
