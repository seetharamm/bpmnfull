package com.bpmnengine.engine.expression;

import java.util.Map;

/**
 * Evaluates expressions in the BPMN engine (conditions, assignments, result values).
 */
public interface ExpressionEvaluator {

    /**
     * Evaluate an expression string against the given variable map.
     *
     * @param expression the expression, e.g. {@code ${amount > 100}} or {@code #{myBean.value}}
     * @param variables  the variable context
     * @return the evaluated result
     */
    Object evaluate(String expression, Map<String, Object> variables);
}
