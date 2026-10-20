package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** BPMN 2.0 &lt;sendTask&gt; — sends a message and continues. */
@Data @EqualsAndHashCode(callSuper = true)
public class SendTask extends Activity {
    private String messageRef;
    private String messageName;
    private String implementation;
    public SendTask() { this.elementType = BpmnElementType.SEND_TASK; }
}
