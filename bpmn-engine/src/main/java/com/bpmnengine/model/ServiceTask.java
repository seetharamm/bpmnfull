package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * BPMN 2.0 &lt;serviceTask&gt;.
 * Invokes an external service via a JavaDelegate, expression, or REST call.
 */
@Data @EqualsAndHashCode(callSuper = true)
public class ServiceTask extends Activity {

    /** Fully-qualified class name implementing JavaDelegate. */
    private String implementation;

    /** Expression to evaluate (e.g. ${myBean.execute(execution)}). */
    private String expression;

    /** Delegate expression (e.g. ${myDelegateBean}). */
    private String delegateExpression;

    /** Result variable to store expression result. */
    private String resultVariable;

    private String topic; // for external worker tasks

    public ServiceTask() { this.elementType = BpmnElementType.SERVICE_TASK; }
}
