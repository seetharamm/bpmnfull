package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** &lt;complexGateway&gt; — custom activation condition. */
@Data @EqualsAndHashCode(callSuper = true)
public class ComplexGateway extends Gateway {
    private String activationCondition;
    private String defaultFlow;
    public ComplexGateway() { this.elementType = BpmnElementType.COMPLEX_GATEWAY; }
}
