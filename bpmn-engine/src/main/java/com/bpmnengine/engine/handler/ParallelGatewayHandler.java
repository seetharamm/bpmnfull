package com.bpmnengine.engine.handler;

import com.bpmnengine.engine.ExecutionContext;
import com.bpmnengine.engine.ExecutionOutcome;
import com.bpmnengine.engine.HistoryService;
import com.bpmnengine.model.*;
import com.bpmnengine.persistence.entity.ExecutionEntity;
import com.bpmnengine.persistence.repository.ExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles &lt;parallelGateway&gt; (AND gateway).
 *
 * Diverging: activates ALL outgoing flows.
 * Converging: waits until all incoming executions have arrived, then continues once.
 */
@Component
public class ParallelGatewayHandler implements FlowElementHandler {

    private static final Logger log = LoggerFactory.getLogger(ParallelGatewayHandler.class);

    private final ExecutionRepository executionRepo;
    private final HistoryService      historyService;

    public ParallelGatewayHandler(ExecutionRepository executionRepo, HistoryService historyService) {
        this.executionRepo  = executionRepo;
        this.historyService = historyService;
    }

    @Override
    public boolean supports(FlowElement element) {
        return element instanceof ParallelGateway;
    }

    @Override
    public ExecutionOutcome execute(ExecutionContext ctx, FlowElement element) {
        ParallelGateway gw = (ParallelGateway) element;

        List<SequenceFlow> outgoing = ctx.getProcess().getOutgoingFlows(gw.getId());
        List<SequenceFlow> incoming = ctx.getProcess().getIncomingFlows(gw.getId());

        if (outgoing.isEmpty()) return ExecutionOutcome.endProcess();

        // ── Diverging (fork) ──────────────────────────────────────────────
        if (incoming.size() <= 1) {
            // Fork: take all outgoing flows
            return ExecutionOutcome.continueOn(new ArrayList<>(outgoing));
        }

        // ── Converging (join) ─────────────────────────────────────────────
        // Count active executions at this gateway node.
        // takeSequenceFlow() already persisted currentFlowNodeId = gw.getId() for the
        // current ctx before execute() was called — so the count already includes the
        // current token. No +1 needed.
        long arrived = executionRepo
                .countByProcessInstanceIdAndCurrentFlowNodeId(ctx.getProcessInstanceId(), gw.getId());

        log.debug("Parallel join {}: {}/{} tokens arrived", gw.getId(), arrived, incoming.size());

        if (arrived < incoming.size()) {
            // Not all branches have arrived yet — park this execution here
            return ExecutionOutcome.waitState();
        }

        // All branches arrived — deactivate all parked executions (the current one will
        // be handled by persistExecutionState after execute() returns continueOn)
        executionRepo.findByProcessInstanceIdAndCurrentFlowNodeId(ctx.getProcessInstanceId(), gw.getId())
                .forEach(exec -> {
                    if (!exec.getId().equals(ctx.getId())) {
                        // Only deactivate the OTHER parked branches, not the current execution
                        // (its state will be updated by persistExecutionState as it continues)
                        exec.setIsActive(false);
                        exec.setState(ExecutionEntity.State.ENDED);
                        executionRepo.save(exec);
                    }
                });

        // Close ALL open history rows for this gateway across every parked branch execution.
        // Each branch recorded a started-but-not-ended row when it parked here.
        historyService.recordActivityEndedForInstance(ctx.getProcessInstanceId(), gw);

        return ExecutionOutcome.continueOn(new ArrayList<>(outgoing));
    }
}
