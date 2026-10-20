package com.bpmnengine.engine.handler;

import com.bpmnengine.engine.ExecutionContext;
import com.bpmnengine.engine.ExecutionOutcome;
import com.bpmnengine.engine.expression.ExpressionEvaluator;
import com.bpmnengine.model.*;
import com.bpmnengine.persistence.entity.EventSubscriptionEntity;
import com.bpmnengine.persistence.repository.EventSubscriptionRepository;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.UUID;

/**
 * Handles intermediate catch events: message, signal, timer, conditional.
 * All of these create a wait state and an event subscription.
 */
@Component
public class IntermediateCatchEventHandler implements FlowElementHandler {

    private final EventSubscriptionRepository subscriptionRepo;
    private final ExpressionEvaluator expressionEvaluator;

    public IntermediateCatchEventHandler(EventSubscriptionRepository subscriptionRepo,
                                          ExpressionEvaluator expressionEvaluator) {
        this.subscriptionRepo    = subscriptionRepo;
        this.expressionEvaluator = expressionEvaluator;
    }

    @Override
    public boolean supports(FlowElement element) {
        return element instanceof IntermediateCatchEvent;
    }

    @Override
    public ExecutionOutcome execute(ExecutionContext ctx, FlowElement element) {
        IntermediateCatchEvent ice = (IntermediateCatchEvent) element;

        if (ice.getEventDefinitions().isEmpty()) {
            // No event definition — plain catch event, just continue
            var flows = ctx.getProcess().getOutgoingFlows(ice.getId());
            return flows.isEmpty() ? ExecutionOutcome.endProcess() : ExecutionOutcome.continueOn(flows.get(0));
        }

        EventDefinition ed = ice.getFirstEventDefinition();
        EventSubscriptionEntity sub = new EventSubscriptionEntity();
        sub.setId(UUID.randomUUID().toString());
        sub.setExecutionId(ctx.getId());
        sub.setProcessInstanceId(ctx.getProcessInstanceId());
        sub.setActivityId(ice.getId());
        sub.setCreatedAt(Instant.now());

        if (ed instanceof MessageEventDefinition med) {
            sub.setEventType("MESSAGE");
            sub.setEventName(med.getMessageName() != null ? med.getMessageName() :
                    resolveMessageName(ctx, med.getMessageRef()));
        } else if (ed instanceof SignalEventDefinition sed) {
            sub.setEventType("SIGNAL");
            sub.setEventName(sed.getSignalName() != null ? sed.getSignalName() :
                    resolveSignalName(ctx, sed.getSignalRef()));
        } else if (ed instanceof ConditionalEventDefinition ced) {
            sub.setEventType("CONDITIONAL");
            sub.setConfiguration(ced.getCondition());
        } else if (ed instanceof TimerEventDefinition ted) {
            // Timer intermediate catch: create a job (handled by timer subsystem)
            sub.setEventType("TIMER");
            sub.setConfiguration(buildTimerConfig(ted));
        }

        subscriptionRepo.save(sub);
        // Engine pauses here; resumed by message/signal/timer
        return ExecutionOutcome.waitState();
    }

    private String resolveMessageName(ExecutionContext ctx, String messageRef) {
        if (messageRef == null) return null;
        return ctx.getBpmnModel().getMessages().stream()
                .filter(m -> messageRef.equals(m.getId()))
                .map(Message::getName)
                .findFirst().orElse(messageRef);
    }

    private String resolveSignalName(ExecutionContext ctx, String signalRef) {
        if (signalRef == null) return null;
        return ctx.getBpmnModel().getSignals().stream()
                .filter(s -> signalRef.equals(s.getId()))
                .map(Signal::getName)
                .findFirst().orElse(signalRef);
    }

    private String buildTimerConfig(TimerEventDefinition ted) {
        if (ted.getTimeDate()     != null) return "date:" + ted.getTimeDate();
        if (ted.getTimeDuration() != null) return "duration:" + ted.getTimeDuration();
        if (ted.getTimeCycle()    != null) return "cycle:" + ted.getTimeCycle();
        return "";
    }
}
