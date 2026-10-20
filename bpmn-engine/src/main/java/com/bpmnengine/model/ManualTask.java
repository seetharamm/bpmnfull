package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** BPMN 2.0 &lt;manualTask&gt; — no automatic execution, purely informational. */
@Data @EqualsAndHashCode(callSuper = true)
public class ManualTask extends Activity {
    public ManualTask() { this.elementType = BpmnElementType.MANUAL_TASK; }
}
