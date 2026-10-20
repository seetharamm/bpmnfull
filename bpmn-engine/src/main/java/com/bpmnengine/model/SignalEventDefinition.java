package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** &lt;signalEventDefinition&gt; */
@Data @EqualsAndHashCode(callSuper = true)
public class SignalEventDefinition extends EventDefinition {
    private String signalRef;
    private String signalName;
}
