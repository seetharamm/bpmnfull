package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** BPMN 2.0 &lt;intermediateCatchEvent&gt;. */
@Data @EqualsAndHashCode(callSuper = true)
public class IntermediateCatchEvent extends Event {
    public IntermediateCatchEvent() { this.elementType = BpmnElementType.INTERMEDIATE_CATCH_EVENT; }
}
