package com.bpmnengine.engine;

import com.bpmnengine.model.*;
import com.bpmnengine.model.Process;
import lombok.Getter;
import lombok.Setter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An execution context (token) that travels along sequence flows.
 * Each execution corresponds to one active path of execution inside a process instance.
 */
@Getter
@Setter
public class ExecutionContext {

    private String id;
    private String processInstanceId;
    private String processDefinitionId;
    private String processDefinitionKey;

    /** The parent execution (for sub-processes / parallel branches). */
    private ExecutionContext parent;

    /** The process model in which this execution runs. */
    private BpmnModel bpmnModel;

    /** The specific process (or sub-process) scope. */
    private Process process;

    /** The current flow element ID being executed. */
    private String currentFlowElementId;

    /** Execution-scoped variables. */
    private Map<String, Object> variables = new ConcurrentHashMap<>();

    private boolean scope        = false;
    private boolean concurrent   = false;
    private boolean active       = true;
    private boolean eventScope   = false;
    private boolean ended        = false;

    /** Number of concurrent executions that joined into a gateway (for parallel/inclusive). */
    private int joinCount = 0;

    /** Required no-arg constructor for frameworks. */
    public ExecutionContext() {}

    public ExecutionContext(String id, String processInstanceId,
                            String processDefinitionId, String processDefinitionKey) {
        this.id                  = id;
        this.processInstanceId   = processInstanceId;
        this.processDefinitionId = processDefinitionId;
        this.processDefinitionKey = processDefinitionKey;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Variable access
    // ──────────────────────────────────────────────────────────────────────────

    public void setVariable(String name, Object value) {
        if (scope || parent == null) {
            variables.put(name, value);
        } else {
            parent.setVariable(name, value);
        }
    }

    public Object getVariable(String name) {
        if (variables.containsKey(name)) return variables.get(name);
        if (parent != null) return parent.getVariable(name);
        return null;
    }

    public boolean hasVariable(String name) {
        return variables.containsKey(name) || (parent != null && parent.hasVariable(name));
    }

    /** Returns all variables visible from this execution (merged up the scope chain). */
    public Map<String, Object> getAllVariables() {
        Map<String, Object> all = new HashMap<>();
        if (parent != null) all.putAll(parent.getAllVariables());
        all.putAll(variables); // local overrides parent
        return all;
    }

    public void setVariableLocal(String name, Object value) {
        variables.put(name, value);
    }

    public Object getVariableLocal(String name) {
        return variables.get(name);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Navigation
    // ──────────────────────────────────────────────────────────────────────────

    public FlowElement getCurrentFlowElement() {
        if (currentFlowElementId == null || process == null) return null;
        return process.getFlowElement(currentFlowElementId);
    }

    public void setCurrentFlowElement(FlowElement fe) {
        this.currentFlowElementId = fe != null ? fe.getId() : null;
    }

    public void end() {
        this.active = false;
        this.ended  = true;
    }
}
