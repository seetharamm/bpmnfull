package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** Generic &lt;task&gt;. */
@Data @EqualsAndHashCode(callSuper = true)
public class TaskElement extends Activity {
    public TaskElement() { this.elementType = BpmnElementType.TASK; }
}
