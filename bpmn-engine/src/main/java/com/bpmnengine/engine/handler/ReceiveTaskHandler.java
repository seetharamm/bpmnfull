package com.bpmnengine.engine.handler;

import com.bpmnengine.engine.ExecutionContext;
import com.bpmnengine.engine.ExecutionOutcome;
import com.bpmnengine.engine.ProcessEngine;
import com.bpmnengine.model.*;
import com.bpmnengine.persistence.entity.EventSubscriptionEntity;
import com.bpmnengine.persistence.repository.EventSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Handles &lt;receiveTask&gt; — creates a message subscription and enters wait state.
 */
@Component
public class ReceiveTaskHandler implements FlowElementHandler {

    private static final Logger log = LoggerFactory.getLogger(ReceiveTaskHandler.class);

    private final EventSubscriptionRepository subscriptionRepo;

    public ReceiveTaskHandler(EventSubscriptionRepository subscriptionRepo) {
        this.subscriptionRepo = subscriptionRepo;
    }

    @Override
    public boolean supports(FlowElement element) {
        return element instanceof ReceiveTask;
    }

    @Override
    public ExecutionOutcome execute(ExecutionContext ctx, FlowElement element) {
        ReceiveTask rt = (ReceiveTask) element;

        // Check if we are resuming (subscription already triggered)
        boolean hasActiveSub = subscriptionRepo
                .findByExecutionIdAndEventType(ctx.getId(), "MESSAGE").isEmpty();

        if (!hasActiveSub) {
            // Resuming after message received — follow outgoing flows
            List<SequenceFlow> flows = ctx.getProcess().getOutgoingFlows(rt.getId());
            return flows.isEmpty() ? ExecutionOutcome.endProcess() : ExecutionOutcome.continueOn(flows.get(0));
        }

        // Create subscription
        EventSubscriptionEntity sub = new EventSubscriptionEntity();
        sub.setId(UUID.randomUUID().toString());
        sub.setExecutionId(ctx.getId());
        sub.setProcessInstanceId(ctx.getProcessInstanceId());
        sub.setActivityId(rt.getId());
        sub.setEventType("MESSAGE");
        sub.setEventName(rt.getMessageName());
        sub.setCreatedAt(Instant.now());
        subscriptionRepo.save(sub);

        log.debug("ReceiveTask {}: waiting for message '{}'", rt.getId(), rt.getMessageName());
        return ExecutionOutcome.waitState();
    }
}
