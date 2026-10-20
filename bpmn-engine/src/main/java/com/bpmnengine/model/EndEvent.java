package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** BPMN 2.0 &lt;endEvent&gt;. */
@Data @EqualsAndHashCode(callSuper = true)
public class EndEvent extends Event {
    private boolean isTerminate = false;
    public EndEvent() { this.elementType = BpmnElementType.END_EVENT; }
}
