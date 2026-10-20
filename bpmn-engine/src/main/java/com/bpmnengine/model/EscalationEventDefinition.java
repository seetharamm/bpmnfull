package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** &lt;escalationEventDefinition&gt; */
@Data @EqualsAndHashCode(callSuper = true)
public class EscalationEventDefinition extends EventDefinition {
    private String escalationRef;
    private String escalationCode;
    private String escalationCodeVariable;
}
