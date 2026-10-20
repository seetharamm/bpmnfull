package com.bpmnengine.engine.handler;

import com.bpmnengine.engine.ExecutionContext;
import com.bpmnengine.engine.ExecutionOutcome;
import com.bpmnengine.model.FlowElement;

/**
 * Contract that every BPMN element handler must implement.
 * A handler receives the current execution and the element to execute,
 * and returns an {@link ExecutionOutcome} describing what to do next.
 */
public interface FlowElementHandler {

    /** The BPMN element type(s) this handler is responsible for. */
    boolean supports(FlowElement element);

    /**
     * Execute the given element.
     *
     * @param ctx     the current execution context (mutable)
     * @param element the flow element to execute
     * @return outcome describing next state (wait, continue on flows, or end)
     */
    ExecutionOutcome execute(ExecutionContext ctx, FlowElement element);
}
