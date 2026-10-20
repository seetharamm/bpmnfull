package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** Base class for all BPMN gateways. */
@Data @EqualsAndHashCode(callSuper = true)
public abstract class Gateway extends FlowElement {
    private GatewayDirection gatewayDirection = GatewayDirection.UNSPECIFIED;

    public enum GatewayDirection { UNSPECIFIED, CONVERGING, DIVERGING, MIXED }
}
