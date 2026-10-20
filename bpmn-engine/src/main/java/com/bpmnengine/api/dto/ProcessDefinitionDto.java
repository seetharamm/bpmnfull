package com.bpmnengine.api.dto;

import lombok.Data;
import java.time.Instant;

/**
 * Public view of a deployed process definition.
 * The {@code bpmnXml} field is omitted from list responses but included in single-item GETs.
 */
@Data
public class ProcessDefinitionDto {
    private String  id;
    private String  key;
    private int     version;
    private String  name;
    private String  description;
    private boolean suspended;
    /** True = this is the version that will be used when starting by key. */
    private boolean active;
    private String  resourceName;
    private String  deploymentId;
    private String  deployedBy;
    private String  deployMessage;
    private Instant createdAt;
    private Instant updatedAt;
    /** Full BPMN 2.0 XML — only included when explicitly requested. */
    private String  bpmnXml;
    /** Count of running instances at this version — populated on demand. */
    private Long    runningInstanceCount;
}
