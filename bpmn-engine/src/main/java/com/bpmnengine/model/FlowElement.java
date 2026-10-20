package com.bpmnengine.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all BPMN flow elements (events, tasks, gateways, sequence flows).
 */
@Data
public abstract class FlowElement {

    protected String id;
    protected String name;
    protected String documentation;
    protected BpmnElementType elementType;

    /** Extension properties (custom engine-specific attributes). */
    protected ExtensionElements extensionElements;

    /** Incoming sequence flow IDs. */
    protected List<String> incomingFlows = new ArrayList<>();

    /** Outgoing sequence flow IDs. */
    protected List<String> outgoingFlows = new ArrayList<>();
}
