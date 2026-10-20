package com.bpmnengine.identity.service;

/** Thrown when an identity operation fails (duplicate username, missing entity, etc.). */
public class IdentityException extends RuntimeException {
    public IdentityException(String message) { super(message); }
    public IdentityException(String message, Throwable cause) { super(message, cause); }
}
