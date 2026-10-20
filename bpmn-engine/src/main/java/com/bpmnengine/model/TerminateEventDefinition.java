package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** &lt;terminateEventDefinition&gt; — for end events. */
@Data @EqualsAndHashCode(callSuper = true)
public class TerminateEventDefinition extends EventDefinition {
    private boolean terminateAll = false;
}
