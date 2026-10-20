package com.bpmnengine.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request body for the deploy endpoint (version-aware). */
@Data
public class DeployProcessRequest {
    /** Human-readable resource name (e.g. "order-process-v3.bpmn20.xml"). */
    private String resourceName;

    /** Raw BPMN 2.0 XML document. */
    @NotBlank(message = "bpmnXml must not be blank")
    private String bpmnXml;

    /** Username of the person performing the deployment (optional). */
    private String deployedBy;

    /** Optional release note / changelog entry for this version. */
    private String deployMessage;
}
