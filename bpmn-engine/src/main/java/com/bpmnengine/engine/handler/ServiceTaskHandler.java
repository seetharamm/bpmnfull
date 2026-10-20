package com.bpmnengine.engine.handler;

import com.bpmnengine.engine.EngineException;
import com.bpmnengine.engine.ExecutionContext;
import com.bpmnengine.engine.ExecutionOutcome;
import com.bpmnengine.engine.delegate.JavaDelegate;
import com.bpmnengine.engine.expression.ExpressionEvaluator;
import com.bpmnengine.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Handles &lt;serviceTask&gt; by delegating to a {@link JavaDelegate}, a Spring expression,
 * or a delegate expression.
 */
@Component
public class ServiceTaskHandler implements FlowElementHandler {

    private static final Logger log = LoggerFactory.getLogger(ServiceTaskHandler.class);

    private final ApplicationContext  applicationContext;
    private final ExpressionEvaluator expressionEvaluator;

    public ServiceTaskHandler(ApplicationContext applicationContext,
                               ExpressionEvaluator expressionEvaluator) {
        this.applicationContext  = applicationContext;
        this.expressionEvaluator = expressionEvaluator;
    }

    @Override
    public boolean supports(FlowElement element) {
        return element instanceof ServiceTask
                || element instanceof SendTask
                || element instanceof BusinessRuleTask
                || element instanceof ManualTask
                || element instanceof TaskElement;
    }

    @Override
    public ExecutionOutcome execute(ExecutionContext ctx, FlowElement element) {
        if (element instanceof ServiceTask st) {
            executeServiceTask(ctx, st);
        } else if (element instanceof BusinessRuleTask brt) {
            executeBusinessRule(ctx, brt);
        }
        // ManualTask and generic Task — no automatic action, just pass through
        List<SequenceFlow> flows = ctx.getProcess().getOutgoingFlows(element.getId());
        return flows.isEmpty() ? ExecutionOutcome.endProcess() : ExecutionOutcome.continueOn(flows.get(0));
    }

    private void executeServiceTask(ExecutionContext ctx, ServiceTask st) {
        if (st.getImplementation() != null && !st.getImplementation().isBlank()) {
            try {
                Class<?> cls = Class.forName(st.getImplementation());
                Object bean = tryGetBean(cls);
                if (bean instanceof JavaDelegate delegate) {
                    try {
                        delegate.execute(ctx);
                    } catch (Exception e) {
                        throw new EngineException("JavaDelegate failed: " + e.getMessage(), e);
                    }
                } else {
                    log.warn("Class {} does not implement JavaDelegate", st.getImplementation());
                }
            } catch (ClassNotFoundException e) {
                log.warn("Delegate class not found: {}", st.getImplementation());
            }
        } else if (st.getExpression() != null && !st.getExpression().isBlank()) {
            Object result = expressionEvaluator.evaluate(st.getExpression(), ctx.getAllVariables());
            if (st.getResultVariable() != null) ctx.setVariable(st.getResultVariable(), result);
        } else if (st.getDelegateExpression() != null && !st.getDelegateExpression().isBlank()) {
            Object bean = expressionEvaluator.evaluate(st.getDelegateExpression(), ctx.getAllVariables());
            if (bean instanceof JavaDelegate delegate) {
                try {
                    delegate.execute(ctx);
                } catch (Exception e) {
                    throw new EngineException("DelegateExpression failed: " + e.getMessage(), e);
                }
            }
        }
    }

    private void executeBusinessRule(ExecutionContext ctx, BusinessRuleTask brt) {
        log.warn("BusinessRuleTask '{}' — no DMN engine configured; skipping", brt.getId());
    }

    private Object tryGetBean(Class<?> cls) {
        try {
            return applicationContext.getBean(cls);
        } catch (Exception e) {
            try { return cls.getDeclaredConstructor().newInstance(); }
            catch (Exception ex) { return null; }
        }
    }
}
