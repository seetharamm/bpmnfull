package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** BPMN 2.0 &lt;scriptTask&gt;. */
@Data @EqualsAndHashCode(callSuper = true)
public class ScriptTask extends Activity {

    private String scriptFormat = "javascript"; // javascript | groovy | juel
    private String script;
    private String resultVariable;

    public ScriptTask() { this.elementType = BpmnElementType.SCRIPT_TASK; }
}
