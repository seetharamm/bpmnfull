package com.bpmnengine.engine;

import com.bpmnengine.model.SequenceFlow;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Describes the outcome of executing a flow element.
 */
@Data
public class ExecutionOutcome {

    /** If true the execution should pause here (wait state). */
    private boolean wait = false;

    /** If true the execution has reached an end and the process should complete. */
    private boolean end = false;

    /** Outgoing sequence flows to follow after this element. */
    private List<SequenceFlow> outgoingFlows = new ArrayList<>();

    // ── Factory methods ─────────────────────────────────────────────────────

    public static ExecutionOutcome waitState() {
        ExecutionOutcome o = new ExecutionOutcome();
        o.setWait(true);
        return o;
    }

    public static ExecutionOutcome endProcess() {
        ExecutionOutcome o = new ExecutionOutcome();
        o.setEnd(true);
        return o;
    }

    public static ExecutionOutcome continueOn(List<SequenceFlow> flows) {
        ExecutionOutcome o = new ExecutionOutcome();
        o.setOutgoingFlows(new ArrayList<>(flows));
        return o;
    }

    public static ExecutionOutcome continueOn(SequenceFlow flow) {
        return continueOn(List.of(flow));
    }
}
