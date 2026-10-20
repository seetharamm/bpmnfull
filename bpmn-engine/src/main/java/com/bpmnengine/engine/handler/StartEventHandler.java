package com.bpmnengine.engine.handler;

import com.bpmnengine.engine.ExecutionContext;
import com.bpmnengine.engine.ExecutionOutcome;
import com.bpmnengine.model.*;
import org.springframework.stereotype.Component;

/**
 * Handles start events (none, message, timer, signal, conditional).
 * Simply passes execution through to the first outgoing sequence flow.
 */
@Component
public class StartEventHandler implements FlowElementHandler {

    @Override
    public boolean supports(FlowElement element) {
        return element instanceof StartEvent;
    }

    @Override
    public ExecutionOutcome execute(ExecutionContext ctx, FlowElement element) {
        StartEvent event = (StartEvent) element;

        // All start event types: none, message, timer, signal
        // For process startup, simply follow outgoing flows.
        // For sub-process event start events, same logic applies.
        var flows = ctx.getProcess().getOutgoingFlows(event.getId());
        if (flows.isEmpty()) {
            return ExecutionOutcome.endProcess();
        }
        return ExecutionOutcome.continueOn(flows.get(0));
    }
}
