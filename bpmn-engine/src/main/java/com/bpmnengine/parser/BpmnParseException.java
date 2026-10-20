package com.bpmnengine.parser;

/** Thrown when a BPMN XML document cannot be parsed. */
public class BpmnParseException extends RuntimeException {
    public BpmnParseException(String message) { super(message); }
    public BpmnParseException(String message, Throwable cause) { super(message, cause); }
}
