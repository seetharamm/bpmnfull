package com.bpmnengine.engine;

/** Thrown by the engine when an unrecoverable error occurs during execution. */
public class EngineException extends RuntimeException {
    public EngineException(String message) { super(message); }
    public EngineException(String message, Throwable cause) { super(message, cause); }
}
