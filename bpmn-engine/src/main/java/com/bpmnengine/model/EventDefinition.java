package com.bpmnengine.model;

import lombok.Data;

/** Base class for all BPMN event definitions. */
@Data
public abstract class EventDefinition {
    protected String id;
}
