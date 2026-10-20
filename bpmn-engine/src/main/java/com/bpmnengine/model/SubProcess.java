package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.ArrayList;
import java.util.List;

/**
 * BPMN 2.0 &lt;subProcess&gt; — an embedded subprocess containing its own flow elements.
 */
@Data @EqualsAndHashCode(callSuper = true)
public class SubProcess extends Activity {

    private boolean triggeredByEvent = false;

    /** Inner flow elements. */
    private List<FlowElement> flowElements = new ArrayList<>();
    private List<SequenceFlow> sequenceFlows = new ArrayList<>();

    public SubProcess() { this.elementType = BpmnElementType.SUB_PROCESS; }

    public FlowElement getFlowElement(String id) {
        return flowElements.stream()
                .filter(fe -> id.equals(fe.getId()))
                .findFirst().orElse(null);
    }
}
