package com.bpmnengine.engine.expression;

/** Thrown when expression evaluation fails. */
public class ExpressionEvaluationException extends RuntimeException {
    public ExpressionEvaluationException(String msg, Throwable cause) { super(msg, cause); }
}
