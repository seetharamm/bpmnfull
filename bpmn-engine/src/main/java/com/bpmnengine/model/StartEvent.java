package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** BPMN 2.0 &lt;startEvent&gt;. */
@Data @EqualsAndHashCode(callSuper = true)
public class StartEvent extends Event {
    public StartEvent() { this.elementType = BpmnElementType.START_EVENT; }
}
