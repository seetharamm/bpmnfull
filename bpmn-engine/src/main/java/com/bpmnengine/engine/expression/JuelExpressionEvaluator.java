package com.bpmnengine.engine.expression;

import de.odysseus.el.ExpressionFactoryImpl;
import de.odysseus.el.util.SimpleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.el.*;
import java.util.Map;

/**
 * JUEL (Java Unified Expression Language) based expression evaluator.
 *
 * Supports both {@code ${...}} (immediate) and {@code #{...}} (deferred) expressions.
 * Variables from the process context are bound as EL variables.
 *
 * Examples:
 *   {@code ${amount > 1000}}          → Boolean
 *   {@code ${customer.name}}          → String
 *   {@code ${score * 2 + bonus}}      → Number
 */
@Component
public class JuelExpressionEvaluator implements ExpressionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(JuelExpressionEvaluator.class);

    private final ExpressionFactory factory;

    public JuelExpressionEvaluator() {
        this.factory = new ExpressionFactoryImpl();
    }

    @Override
    public Object evaluate(String expression, Map<String, Object> variables) {
        if (expression == null || expression.isBlank()) return null;

        // Normalise: wrap plain expressions without ${ / #{
        String expr = expression.trim();
        if (!expr.startsWith("${") && !expr.startsWith("#{")) {
            expr = "${" + expr + "}";
        }

        try {
            SimpleContext context = new SimpleContext();

            // Bind all variables into EL context
            if (variables != null) {
                for (Map.Entry<String, Object> entry : variables.entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        context.setVariable(entry.getKey(),
                                factory.createValueExpression(entry.getValue(), Object.class));
                    }
                }
            }

            ValueExpression ve = factory.createValueExpression(context, expr, Object.class);
            return ve.getValue(context);

        } catch (ELException e) {
            log.warn("Expression evaluation failed [{}]: {}", expression, e.getMessage());
            throw new ExpressionEvaluationException("Failed to evaluate: " + expression, e);
        }
    }
}
