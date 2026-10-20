package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all BPMN events (start, end, intermediate, boundary).
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class Event extends FlowElement {

    protected boolean isInterrupting = true;
    protected List<EventDefinition> eventDefinitions = new ArrayList<>();

    public EventDefinition getFirstEventDefinition() {
        return eventDefinitions.isEmpty() ? null : eventDefinitions.get(0);
    }

    public boolean hasEventDefinition(Class<? extends EventDefinition> type) {
        return eventDefinitions.stream().anyMatch(type::isInstance);
    }
}
