package com.bpmnengine.engine.handler;

import com.bpmnengine.engine.ExecutionContext;
import com.bpmnengine.engine.ExecutionOutcome;
import com.bpmnengine.engine.ProcessEngineImpl;
import com.bpmnengine.engine.ProcessInstanceResult;
import com.bpmnengine.model.*;
import com.bpmnengine.persistence.entity.ProcessInstanceEntity;
import com.bpmnengine.persistence.repository.ProcessDefinitionRepository;
import com.bpmnengine.persistence.repository.ProcessInstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

/**
 * Handles &lt;callActivity&gt; — synchronously calls another process definition
 * and resumes when the called process completes.
 */
@Component
public class CallActivityHandler implements FlowElementHandler {

    private static final Logger log = LoggerFactory.getLogger(CallActivityHandler.class);

    private final ProcessEngineImpl engine;
    private final ProcessDefinitionRepository definitionRepo;
    private final ProcessInstanceRepository instanceRepo;

    public CallActivityHandler(@Lazy ProcessEngineImpl engine,
                                ProcessDefinitionRepository definitionRepo,
                                ProcessInstanceRepository instanceRepo) {
        this.engine         = engine;
        this.definitionRepo = definitionRepo;
        this.instanceRepo   = instanceRepo;
    }

    @Override
    public boolean supports(FlowElement element) {
        return element instanceof CallActivity;
    }

    @Override
    public ExecutionOutcome execute(ExecutionContext ctx, FlowElement element) {
        CallActivity ca = (CallActivity) element;

        if (ca.getCalledElement() == null || ca.getCalledElement().isBlank()) {
            log.warn("CallActivity {} has no calledElement — skipping", ca.getId());
        } else {
            Map<String, Object> vars = ca.isInheritVariables() ? ctx.getAllVariables() : Map.of();
            ProcessInstanceResult result = engine.startProcessByKey(ca.getCalledElement(), vars);
            log.info("CallActivity {}: called process {} → instanceId={}", ca.getId(),
                    ca.getCalledElement(), result.getId());

            // If the called process completed synchronously, continue.
            // If it's in a wait state, this execution must also wait.
            if (!"COMPLETED".equals(result.getState())) {
                ctx.setVariableLocal("__calledInstanceId__", result.getId());
                return ExecutionOutcome.waitState();
            }
        }

        List<SequenceFlow> flows = ctx.getProcess().getOutgoingFlows(ca.getId());
        return flows.isEmpty() ? ExecutionOutcome.endProcess() : ExecutionOutcome.continueOn(flows.get(0));
    }
}
