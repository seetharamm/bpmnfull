package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all BPMN tasks and sub-processes.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class Activity extends FlowElement {

    private boolean asynchronous = false;
    private boolean exclusive = true;
    private boolean forCompensation = false;
    private int loopCardinality = -1;

    /** &lt;multiInstanceLoopCharacteristics&gt; */
    private MultiInstanceLoopCharacteristics multiInstanceLoopCharacteristics;

    /** &lt;standardLoopCharacteristics&gt; */
    private StandardLoopCharacteristics standardLoopCharacteristics;

    /** Boundary events attached to this activity. */
    private List<BoundaryEvent> boundaryEvents = new ArrayList<>();

    /** Data input/output associations. */
    private List<DataAssociation> dataInputAssociations = new ArrayList<>();
    private List<DataAssociation> dataOutputAssociations = new ArrayList<>();

    private IoSpecification ioSpecification;
}
