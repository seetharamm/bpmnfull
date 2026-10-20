package com.bpmnengine.engine.handler;

import com.bpmnengine.engine.ExecutionContext;
import com.bpmnengine.engine.ExecutionOutcome;
import com.bpmnengine.model.*;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Handles intermediate throw events: none, signal, message, escalation, compensate.
 * Throw events fire-and-forget (no wait state).
 */
@Component
public class IntermediateThrowEventHandler implements FlowElementHandler {

    @Override
    public boolean supports(FlowElement element) {
        return element instanceof IntermediateThrowEvent;
    }

    @Override
    public ExecutionOutcome execute(ExecutionContext ctx, FlowElement element) {
        IntermediateThrowEvent ite = (IntermediateThrowEvent) element;

        if (!ite.getEventDefinitions().isEmpty()) {
            EventDefinition ed = ite.getFirstEventDefinition();
            if (ed instanceof SignalEventDefinition) {
                // Actual signal broadcast is handled by the engine via the subscription system
                ctx.setVariable("__throwSignal__", ((SignalEventDefinition) ed).getSignalName());
            } else if (ed instanceof EscalationEventDefinition) {
                ctx.setVariable("__throwEscalation__", ((EscalationEventDefinition) ed).getEscalationCode());
            } else if (ed instanceof CompensateEventDefinition) {
                // Compensation: mark as compensating
                ctx.setVariable("__compensate__", true);
            }
        }

        List<SequenceFlow> flows = ctx.getProcess().getOutgoingFlows(ite.getId());
        return flows.isEmpty() ? ExecutionOutcome.endProcess() : ExecutionOutcome.continueOn(flows.get(0));
    }
}
