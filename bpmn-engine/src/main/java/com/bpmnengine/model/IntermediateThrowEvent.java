package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** BPMN 2.0 &lt;intermediateThrowEvent&gt;. */
@Data @EqualsAndHashCode(callSuper = true)
public class IntermediateThrowEvent extends Event {
    public IntermediateThrowEvent() { this.elementType = BpmnElementType.INTERMEDIATE_THROW_EVENT; }
}
