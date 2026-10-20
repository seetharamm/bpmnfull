package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** BPMN 2.0 &lt;businessRuleTask&gt;. */
@Data @EqualsAndHashCode(callSuper = true)
public class BusinessRuleTask extends Activity {
    private String decisionRef;       // DMN decision reference
    private String resultVariable;
    public BusinessRuleTask() { this.elementType = BpmnElementType.BUSINESS_RULE_TASK; }
}
