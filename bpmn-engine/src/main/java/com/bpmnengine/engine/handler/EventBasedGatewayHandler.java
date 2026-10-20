package com.bpmnengine.engine.handler;

import com.bpmnengine.engine.ExecutionContext;
import com.bpmnengine.engine.ExecutionOutcome;
import com.bpmnengine.model.*;
import com.bpmnengine.persistence.entity.EventSubscriptionEntity;
import com.bpmnengine.persistence.repository.EventSubscriptionRepository;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.UUID;

/**
 * Handles &lt;eventBasedGateway&gt;.
 * Creates event subscriptions for all outgoing elements (message/signal/timer intermediate events)
 * and enters a wait state. When one event fires, the others are cancelled.
 */
@Component
public class EventBasedGatewayHandler implements FlowElementHandler {

    private final EventSubscriptionRepository subscriptionRepo;

    public EventBasedGatewayHandler(EventSubscriptionRepository subscriptionRepo) {
        this.subscriptionRepo = subscriptionRepo;
    }

    @Override
    public boolean supports(FlowElement element) {
        return element instanceof EventBasedGateway || element instanceof ComplexGateway;
    }

    @Override
    public ExecutionOutcome execute(ExecutionContext ctx, FlowElement element) {
        if (element instanceof ComplexGateway) {
            // Complex gateway: just continue (custom activation logic not yet implemented)
            var flows = ctx.getProcess().getOutgoingFlows(element.getId());
            return flows.isEmpty() ? ExecutionOutcome.endProcess() : ExecutionOutcome.continueOn(flows.get(0));
        }

        EventBasedGateway gw = (EventBasedGateway) element;
        var outgoing = ctx.getProcess().getOutgoingFlows(gw.getId());

        // Register a subscription for each following event
        for (SequenceFlow sf : outgoing) {
            FlowElement target = ctx.getProcess().getFlowElement(sf.getTargetRef());
            if (!(target instanceof IntermediateCatchEvent ice)) continue;

            EventDefinition ed = ice.getFirstEventDefinition();
            if (ed == null) continue;

            EventSubscriptionEntity sub = new EventSubscriptionEntity();
            sub.setId(UUID.randomUUID().toString());
            sub.setExecutionId(ctx.getId());
            sub.setProcessInstanceId(ctx.getProcessInstanceId());
            sub.setActivityId(gw.getId());
            sub.setConfiguration(sf.getId()); // store flow id so we know which branch to take
            sub.setCreatedAt(Instant.now());

            if (ed instanceof MessageEventDefinition med) {
                sub.setEventType("MESSAGE");
                sub.setEventName(med.getMessageName());
            } else if (ed instanceof SignalEventDefinition sed) {
                sub.setEventType("SIGNAL");
                sub.setEventName(sed.getSignalName());
            } else if (ed instanceof TimerEventDefinition) {
                sub.setEventType("TIMER");
            }

            subscriptionRepo.save(sub);
        }

        return ExecutionOutcome.waitState();
    }
}
