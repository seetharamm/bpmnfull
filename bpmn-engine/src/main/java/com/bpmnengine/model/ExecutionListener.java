package com.bpmnengine.model;

import lombok.Data;

@Data
public class ExecutionListener {
    public enum Event { START, END, TAKE }
    private Event event;
    private String implementation; // class name
    private String expression;
    private String delegateExpression;
}
