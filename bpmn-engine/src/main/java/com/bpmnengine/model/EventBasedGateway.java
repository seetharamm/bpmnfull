package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** &lt;eventBasedGateway&gt; — waits for first event (message/signal/timer) to arrive. */
@Data @EqualsAndHashCode(callSuper = true)
public class EventBasedGateway extends Gateway {
    private boolean instantiate = false;
    private EventBasedGatewayType eventGatewayType = EventBasedGatewayType.EXCLUSIVE;
    public EventBasedGateway() { this.elementType = BpmnElementType.EVENT_BASED_GATEWAY; }

    public enum EventBasedGatewayType { EXCLUSIVE, PARALLEL }
}
