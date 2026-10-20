package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * BPMN 2.0 &lt;boundaryEvent&gt;.
 * Attached to an activity (hostActivityId) and may be interrupting or non-interrupting.
 */
@Data @EqualsAndHashCode(callSuper = true)
public class BoundaryEvent extends Event {

    /** The id of the activity this boundary event is attached to. */
    private String attachedToRef;
    private boolean cancelActivity = true; // true = interrupting

    public BoundaryEvent() { this.elementType = BpmnElementType.BOUNDARY_EVENT; }
}
