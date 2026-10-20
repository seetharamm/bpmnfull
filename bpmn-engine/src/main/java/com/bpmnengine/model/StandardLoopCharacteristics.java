package com.bpmnengine.model;

import lombok.Data;

/** BPMN 2.0 &lt;standardLoopCharacteristics&gt;. */
@Data
public class StandardLoopCharacteristics {
    private boolean testBefore = false;
    private String loopCondition;
    private int loopMaximum = -1;
}
