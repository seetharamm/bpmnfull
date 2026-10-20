package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** BPMN 2.0 &lt;callActivity&gt; — calls another process or global task. */
@Data @EqualsAndHashCode(callSuper = true)
public class CallActivity extends Activity {

    /** The key of the called process definition. */
    private String calledElement;
    private boolean inheritVariables = false;

    public CallActivity() { this.elementType = BpmnElementType.CALL_ACTIVITY; }
}
