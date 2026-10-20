package com.bpmnengine.engine.handler;

import com.bpmnengine.engine.ExecutionContext;
import com.bpmnengine.engine.ExecutionOutcome;
import com.bpmnengine.model.*;
import org.springframework.stereotype.Component;

/**
 * Handles end events: none, terminate, error, escalation, message, signal, compensate.
 */
@Component
public class EndEventHandler implements FlowElementHandler {

    @Override
    public boolean supports(FlowElement element) {
        return element instanceof EndEvent;
    }

    @Override
    public ExecutionOutcome execute(ExecutionContext ctx, FlowElement element) {
        EndEvent endEvent = (EndEvent) element;

        // Terminate event: end all sibling executions too
        if (endEvent.hasEventDefinition(TerminateEventDefinition.class)) {
            ctx.end();
            return ExecutionOutcome.endProcess();
        }

        // Error end event: propagate as exception
        if (endEvent.hasEventDefinition(ErrorEventDefinition.class)) {
            ErrorEventDefinition eed = (ErrorEventDefinition) endEvent.getFirstEventDefinition();
            ctx.setVariable("__errorCode__", eed.getErrorCode());
            ctx.end();
            return ExecutionOutcome.endProcess();
        }

        // Escalation end event
        if (endEvent.hasEventDefinition(EscalationEventDefinition.class)) {
            EscalationEventDefinition esc = (EscalationEventDefinition) endEvent.getFirstEventDefinition();
            ctx.setVariable("__escalationCode__", esc.getEscalationCode());
            ctx.end();
            return ExecutionOutcome.endProcess();
        }

        // None, message, signal, compensate end events: just end the execution
        ctx.end();
        return ExecutionOutcome.endProcess();
    }
}
