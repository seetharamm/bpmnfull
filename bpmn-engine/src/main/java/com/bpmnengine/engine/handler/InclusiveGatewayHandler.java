package com.bpmnengine.engine.handler;

import com.bpmnengine.engine.ExecutionContext;
import com.bpmnengine.engine.ExecutionOutcome;
import com.bpmnengine.engine.expression.ExpressionEvaluator;
import com.bpmnengine.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles &lt;inclusiveGateway&gt; (OR gateway).
 *
 * Diverging: activates all outgoing flows whose condition evaluates to true
 *            (minimum one; falls back to default).
 * Converging: waits for all active incoming paths to arrive.
 */
@Component
public class InclusiveGatewayHandler implements FlowElementHandler {

    private static final Logger log = LoggerFactory.getLogger(InclusiveGatewayHandler.class);

    private final ExpressionEvaluator expressionEvaluator;

    public InclusiveGatewayHandler(ExpressionEvaluator expressionEvaluator) {
        this.expressionEvaluator = expressionEvaluator;
    }

    @Override
    public boolean supports(FlowElement element) {
        return element instanceof InclusiveGateway;
    }

    @Override
    public ExecutionOutcome execute(ExecutionContext ctx, FlowElement element) {
        InclusiveGateway gw = (InclusiveGateway) element;
        List<SequenceFlow> outgoing = ctx.getProcess().getOutgoingFlows(gw.getId());

        if (outgoing.isEmpty()) return ExecutionOutcome.endProcess();
        if (outgoing.size() == 1) return ExecutionOutcome.continueOn(outgoing.get(0));

        String defaultFlowId = gw.getDefaultFlow();
        SequenceFlow defaultFlow = null;
        List<SequenceFlow> matching = new ArrayList<>();

        for (SequenceFlow sf : outgoing) {
            if (sf.getId().equals(defaultFlowId)) { defaultFlow = sf; continue; }
            if (sf.getConditionExpression() == null || sf.getConditionExpression().isBlank()) {
                matching.add(sf);
            } else if (evaluateCondition(sf, ctx)) {
                matching.add(sf);
            }
        }

        if (!matching.isEmpty()) return ExecutionOutcome.continueOn(matching);
        if (defaultFlow != null) return ExecutionOutcome.continueOn(defaultFlow);
        throw new com.bpmnengine.engine.EngineException(
                "InclusiveGateway '" + gw.getId() + "': no matching path and no default");
    }

    private boolean evaluateCondition(SequenceFlow sf, ExecutionContext ctx) {
        try {
            Object r = expressionEvaluator.evaluate(sf.getConditionExpression(), ctx.getAllVariables());
            return r instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(r));
        } catch (Exception e) {
            log.warn("Condition eval failed on {}: {}", sf.getId(), e.getMessage());
            return false;
        }
    }
}
