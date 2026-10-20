package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** &lt;compensateEventDefinition&gt; */
@Data @EqualsAndHashCode(callSuper = true)
public class CompensateEventDefinition extends EventDefinition {
    private String activityRef;
    private boolean waitForCompletion = true;
}
