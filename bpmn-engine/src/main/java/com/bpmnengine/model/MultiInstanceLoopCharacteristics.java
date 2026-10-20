package com.bpmnengine.model;

import lombok.Data;

/** BPMN 2.0 &lt;multiInstanceLoopCharacteristics&gt;. */
@Data
public class MultiInstanceLoopCharacteristics {
    private boolean sequential = false;
    private String loopCardinality;
    private String inputDataItem;
    private String elementVariable;
    private String completionCondition;
    private String collectionExpression;
}
