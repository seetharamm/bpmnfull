package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** &lt;inclusiveGateway&gt; — OR: one or more outgoing paths. */
@Data @EqualsAndHashCode(callSuper = true)
public class InclusiveGateway extends Gateway {
    private String defaultFlow;
    public InclusiveGateway() { this.elementType = BpmnElementType.INCLUSIVE_GATEWAY; }
}
