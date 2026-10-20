package com.bpmnengine.engine.handler;

import com.bpmnengine.engine.ExecutionContext;
import com.bpmnengine.engine.ExecutionOutcome;
import com.bpmnengine.engine.expression.ExpressionEvaluator;
import com.bpmnengine.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Handles &lt;exclusiveGateway&gt; (XOR gateway).
 *
 * Diverging: evaluates each outgoing flow's condition; takes the first that evaluates
 * to true, or the default flow if none match.
 * Converging: passes through immediately.
 */
@Component
public class ExclusiveGatewayHandler implements FlowElementHandler {

    private static final Logger log = LoggerFactory.getLogger(ExclusiveGatewayHandler.class);

    private final ExpressionEvaluator expressionEvaluator;

    public ExclusiveGatewayHandler(ExpressionEvaluator expressionEvaluator) {
        this.expressionEvaluator = expressionEvaluator;
    }

    @Override
    public boolean supports(FlowElement element) {
        return element instanceof ExclusiveGateway;
    }

    @Override
    public ExecutionOutcome execute(ExecutionContext ctx, FlowElement element) {
        ExclusiveGateway gw = (ExclusiveGateway) element;
        List<SequenceFlow> outgoing = ctx.getProcess().getOutgoingFlows(gw.getId());

        if (outgoing.isEmpty()) return ExecutionOutcome.endProcess();

        // Converging: just pass through
        if (outgoing.size() == 1) return ExecutionOutcome.continueOn(outgoing.get(0));

        // Diverging: evaluate conditions
        String defaultFlowId = gw.getDefaultFlow();
        SequenceFlow defaultFlow = null;

        for (SequenceFlow sf : outgoing) {
            if (sf.getId().equals(defaultFlowId)) {
                defaultFlow = sf;
                continue;
            }
            if (sf.getConditionExpression() == null || sf.getConditionExpression().isBlank()) {
                // No condition — take this path
                log.debug("XOR gateway {}: taking unconditional flow {}", gw.getId(), sf.getId());
                return ExecutionOutcome.continueOn(sf);
            }
            boolean condResult = evaluateCondition(sf, ctx);
            if (condResult) {
                log.debug("XOR gateway {}: condition TRUE on flow {}", gw.getId(), sf.getId());
                return ExecutionOutcome.continueOn(sf);
            }
        }

        // Fall back to default flow
        if (defaultFlow != null) {
            log.debug("XOR gateway {}: using default flow {}", gw.getId(), defaultFlow.getId());
            return ExecutionOutcome.continueOn(defaultFlow);
        }

        throw new com.bpmnengine.engine.EngineException(
                "ExclusiveGateway '" + gw.getId() + "': no condition evaluated to true and no default flow");
    }

    private boolean evaluateCondition(SequenceFlow sf, ExecutionContext ctx) {
        try {
            Object result = expressionEvaluator.evaluate(sf.getConditionExpression(), ctx.getAllVariables());
            if (result instanceof Boolean b) return b;
            return Boolean.parseBoolean(String.valueOf(result));
        } catch (Exception e) {
            log.warn("Condition evaluation failed on flow {}: {}", sf.getId(), e.getMessage());
            return false;
        }
    }
}
