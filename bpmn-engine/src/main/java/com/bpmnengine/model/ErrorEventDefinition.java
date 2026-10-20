package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** &lt;errorEventDefinition&gt; */
@Data @EqualsAndHashCode(callSuper = true)
public class ErrorEventDefinition extends EventDefinition {
    private String errorRef;
    private String errorCode;
    private String errorCodeVariable;
    private String errorMessageVariable;
}
