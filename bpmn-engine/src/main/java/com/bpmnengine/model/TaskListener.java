package com.bpmnengine.model;

import lombok.Data;

@Data
public class TaskListener {
    public enum Event { CREATE, ASSIGNMENT, COMPLETE, DELETE, ALL }
    private Event event;
    private String implementation;
    private String expression;
    private String delegateExpression;
}
