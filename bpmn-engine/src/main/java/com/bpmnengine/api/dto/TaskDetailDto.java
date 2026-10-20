package com.bpmnengine.api.dto;

import lombok.Data;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Rich task detail returned by GET /api/tasks/{taskId}/detail.
 *
 * Contains:
 *  - core task fields
 *  - process-scope variables (visible to all elements in the instance)
 *  - local (execution-scoped) variables set specifically on this task's execution
 *  - I/O declarations from the BPMN model (dataInputAssociations, dataOutputAssociations, IoSpecification)
 */
@Data
public class TaskDetailDto {

    // ── Core task fields ─────────────────────────────────────────────────────

    private String  id;
    private String  name;
    private String  description;
    private String  taskDefinitionKey;
    private String  processInstanceId;
    private String  processDefinitionId;
    private String  processDefinitionKey;
    private String  executionId;
    private String  assignee;
    private String  owner;
    private String  candidateUsers;
    private String  candidateGroups;
    private String  state;
    private int     priority;
    private String  formKey;
    private Instant dueDate;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;

    // ── Variables ────────────────────────────────────────────────────────────

    /** All process-instance-scope variables (shared across the whole instance). */
    private List<VariableDto> processVariables = new ArrayList<>();

    /** Variables scoped specifically to this task's execution token. */
    private List<VariableDto> localVariables   = new ArrayList<>();

    // ── I/O from the BPMN model ──────────────────────────────────────────────

    /** Data inputs declared via &lt;dataInputAssociation&gt; or &lt;ioSpecification&gt;. */
    private List<IoMappingDto> inputs  = new ArrayList<>();

    /** Data outputs declared via &lt;dataOutputAssociation&gt; or &lt;ioSpecification&gt;. */
    private List<IoMappingDto> outputs = new ArrayList<>();

    // ── Nested types ─────────────────────────────────────────────────────────

    @Data
    public static class VariableDto {
        private String name;
        private String type;
        /** String representation of the value (suitable for display). */
        private String value;
        private Instant updatedAt;
    }

    @Data
    public static class IoMappingDto {
        /** Source expression or variable name. */
        private String source;
        /** Target variable name or data input/output name. */
        private String target;
        /** Transformation expression, if any. */
        private String transformation;
    }
}
