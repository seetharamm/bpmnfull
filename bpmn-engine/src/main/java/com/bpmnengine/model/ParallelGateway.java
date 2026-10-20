package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** &lt;parallelGateway&gt; — AND: all incoming must complete; all outgoing are activated. */
@Data @EqualsAndHashCode(callSuper = true)
public class ParallelGateway extends Gateway {
    public ParallelGateway() { this.elementType = BpmnElementType.PARALLEL_GATEWAY; }
}
