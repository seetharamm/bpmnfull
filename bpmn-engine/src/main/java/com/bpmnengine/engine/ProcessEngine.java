package com.bpmnengine.engine;

import com.bpmnengine.api.dto.*;
import java.util.List;
import java.util.Map;

/**
 * Public facade for the BPMN process engine.
 * All interactions with the engine go through this interface.
 */
public interface ProcessEngine {

    // ── Deployment & versioning ───────────────────────────────────────────────

    /**
     * Deploy a BPMN 2.0 XML string.
     * Each deployment of the same process key produces a new version.
     * The new version becomes the active one automatically.
     *
     * @return the new {@link ProcessDefinitionDto}
     */
    ProcessDefinitionDto deployProcess(DeployProcessRequest request);

    /**
     * Legacy overload — deploys with minimal metadata.
     * @return the new definition ID (for backwards compatibility)
     */
    String deployProcess(String resourceName, String bpmnXml);

    // ── Definition queries ────────────────────────────────────────────────────

    /** List all process definitions (one row per key — the active version). */
    List<ProcessDefinitionDto> listDefinitions();

    /** List definitions matching a search query (name or key). */
    List<ProcessDefinitionDto> searchDefinitions(String query);

    /** Get the active definition for a key. */
    ProcessDefinitionDto getActiveDefinition(String processKey);

    /** Get a specific version of a definition. */
    ProcessDefinitionDto getDefinitionByVersion(String processKey, int version);

    /** Get a definition by its unique ID. */
    ProcessDefinitionDto getDefinitionById(String definitionId);

    /** Get a full version history for a process key, newest first. */
    ProcessVersionSummaryDto getVersionHistory(String processKey);

    /** Download the BPMN XML for a specific definition. */
    String getBpmnXml(String processKey, int version);

    // ── Version management ────────────────────────────────────────────────────

    /**
     * Set a specific version as the active version for its key.
     * Starting a process by key will then use this version.
     */
    ProcessDefinitionDto setActiveVersion(String processKey, int version);

    /**
     * Delete a specific version of a process definition.
     * The active version cannot be deleted while it has running instances.
     * When deleting the active version, the previous version becomes active.
     */
    void deleteVersion(String processKey, int version);

    /**
     * Delete ALL versions of a process key.
     * Fails if any version still has running instances.
     */
    void deleteAllVersions(String processKey);

    // ── Suspend / activate a definition ──────────────────────────────────────

    /** Suspend all versions of a process key (no new instances can be started). */
    void suspendProcessDefinition(String processKey);

    /** Re-activate a previously suspended process key. */
    void activateProcessDefinition(String processKey);

    /** Suspend a single specific version. */
    void suspendProcessDefinitionVersion(String processKey, int version);

    /** Re-activate a single specific version. */
    void activateProcessDefinitionVersion(String processKey, int version);

    // ── Start process ─────────────────────────────────────────────────────────

    /** Start a new process instance using the active version of a key. */
    ProcessInstanceResult startProcessByKey(String processKey, Map<String, Object> variables);

    /** Start using the active version, with a business key. */
    ProcessInstanceResult startProcessByKey(String processKey, String businessKey,
                                            Map<String, Object> variables);

    /** Start a specific version. */
    ProcessInstanceResult startProcessByKeyAndVersion(String processKey, int version,
                                                      String businessKey,
                                                      Map<String, Object> variables);

    /** Start by definition ID. */
    ProcessInstanceResult startProcessById(String processDefinitionId,
                                           String businessKey,
                                           Map<String, Object> variables);

    // ── Message / signal ──────────────────────────────────────────────────────

    void correlateMessage(String messageName, String businessKey, Map<String, Object> variables);
    void signalAll(String signalName, Map<String, Object> variables);
    void signalExecution(String executionId, Map<String, Object> variables);

    // ── Task ──────────────────────────────────────────────────────────────────

    void completeTask(String taskId, Map<String, Object> variables);
    void claimTask(String taskId, String userId);
    void triggerTimerJob(String jobId);

    // ── Instance management ───────────────────────────────────────────────────

    /** List all process instances, optionally filtered by state. */
    List<ProcessInstanceResult> listInstances(String state);

    ProcessInstanceResult getProcessInstance(String processInstanceId);
    void suspendProcessInstance(String processInstanceId);
    void activateProcessInstance(String processInstanceId);
    void terminateProcessInstance(String processInstanceId, String reason);
}
