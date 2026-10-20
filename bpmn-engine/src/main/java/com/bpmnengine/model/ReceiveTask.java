package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** BPMN 2.0 &lt;receiveTask&gt; — waits for a message. */
@Data @EqualsAndHashCode(callSuper = true)
public class ReceiveTask extends Activity {
    private String messageRef;
    private String messageName;
    private boolean instantiate = false;
    public ReceiveTask() { this.elementType = BpmnElementType.RECEIVE_TASK; }
}
