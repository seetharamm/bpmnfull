package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** &lt;exclusiveGateway&gt; — XOR: one outgoing path. */
@Data @EqualsAndHashCode(callSuper = true)
public class ExclusiveGateway extends Gateway {
    private String defaultFlow;
    public ExclusiveGateway() { this.elementType = BpmnElementType.EXCLUSIVE_GATEWAY; }
}
