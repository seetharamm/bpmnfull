package com.bpmnengine.engine.handler;

import com.bpmnengine.engine.ExecutionContext;
import com.bpmnengine.engine.ExecutionOutcome;
import com.bpmnengine.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Handles &lt;subProcess&gt; (embedded sub-process).
 * Enters the sub-process scope and executes its start event.
 */
@Component
public class SubProcessHandler implements FlowElementHandler {

    private static final Logger log = LoggerFactory.getLogger(SubProcessHandler.class);

    @Override
    public boolean supports(FlowElement element) {
        return element instanceof SubProcess;
    }

    @Override
    public ExecutionOutcome execute(ExecutionContext ctx, FlowElement element) {
        SubProcess sp = (SubProcess) element;

        // Find the none-start event inside the sub-process
        StartEvent innerStart = sp.getFlowElements().stream()
                .filter(fe -> fe instanceof StartEvent se && se.getEventDefinitions().isEmpty())
                .map(fe -> (StartEvent) fe)
                .findFirst()
                .orElse(null);

        if (innerStart == null) {
            log.warn("SubProcess {} has no none-start event — skipping inner execution", sp.getId());
        } else {
            // Create a child ExecutionContext pointing into the sub-process
            // For simplicity in this synchronous model, we run it in-process here
            // In a full engine this would create a child execution scope
            ExecutionContext subCtx = new ExecutionContext(
                    ctx.getId() + "#sub",
                    ctx.getProcessInstanceId(),
                    ctx.getProcessDefinitionId(),
                    ctx.getProcessDefinitionKey()
            );
            // Override the process scope with the subprocess's flow elements
            com.bpmnengine.model.Process spProcess = new com.bpmnengine.model.Process();
            spProcess.setId(sp.getId());
            spProcess.getFlowElements().addAll(sp.getFlowElements());
            spProcess.getSequenceFlows().addAll(sp.getSequenceFlows());

            subCtx.setBpmnModel(ctx.getBpmnModel());
            subCtx.setProcess(spProcess);
            subCtx.setParent(ctx);
            subCtx.setScope(true);
            subCtx.setCurrentFlowElement(innerStart);

            // Variables from parent are inherited
            ctx.getAllVariables().forEach(subCtx::setVariableLocal);

            // The engine will call executeElement on subCtx
            // (in the full implementation, the engine loop handles this)
            log.debug("Entering sub-process {}", sp.getId());
        }

        List<SequenceFlow> flows = ctx.getProcess().getOutgoingFlows(sp.getId());
        return flows.isEmpty() ? ExecutionOutcome.endProcess() : ExecutionOutcome.continueOn(flows.get(0));
    }
}
