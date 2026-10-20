package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * BPMN 2.0 sequence flow (&lt;sequenceFlow&gt;).
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SequenceFlow extends FlowElement {

    private String sourceRef;
    private String targetRef;
    private String conditionExpression;
    private String conditionLanguage = "juel"; // juel | javascript | groovy
    private boolean isDefault = false;
    private boolean skipExpression = false;

    public SequenceFlow() {
        this.elementType = BpmnElementType.SEQUENCE_FLOW;
    }
}
