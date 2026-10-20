package com.bpmnengine.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a BPMN 2.0 &lt;process&gt; element.
 */
@Data
public class Process {

    private String id;
    private String name;
    private boolean executable = true;
    private boolean closed = false;
    private ProcessType processType = ProcessType.NONE;

    /** Documentation text. */
    private String documentation;

    /** Ordered list of all flow elements (events, tasks, gateways, subprocesses). */
    private List<FlowElement> flowElements = new ArrayList<>();

    /** All sequence flows (also present in flowElements). */
    private List<SequenceFlow> sequenceFlows = new ArrayList<>();

    /** Lanes for visualization (not executed). */
    private List<Lane> lanes = new ArrayList<>();

    /** Data objects scoped to this process. */
    private List<DataObject> dataObjects = new ArrayList<>();

    /** IoSpecification for the process. */
    private IoSpecification ioSpecification;

    public FlowElement getFlowElement(String elementId) {
        return flowElements.stream()
                .filter(fe -> elementId.equals(fe.getId()))
                .findFirst().orElse(null);
    }

    public List<SequenceFlow> getOutgoingFlows(String sourceId) {
        return sequenceFlows.stream()
                .filter(sf -> sourceId.equals(sf.getSourceRef()))
                .collect(java.util.stream.Collectors.toList());
    }

    public List<SequenceFlow> getIncomingFlows(String targetId) {
        return sequenceFlows.stream()
                .filter(sf -> targetId.equals(sf.getTargetRef()))
                .collect(java.util.stream.Collectors.toList());
    }

    public enum ProcessType { NONE, PUBLIC, PRIVATE }
}
