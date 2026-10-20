package com.bpmnengine.model;

/**
 * All BPMN 2.0 flow element types supported by this engine.
 */
public enum BpmnElementType {

    // ── Events ──────────────────────────────────────────────────
    START_EVENT,
    END_EVENT,
    INTERMEDIATE_CATCH_EVENT,
    INTERMEDIATE_THROW_EVENT,
    BOUNDARY_EVENT,

    // ── Tasks ───────────────────────────────────────────────────
    TASK,
    USER_TASK,
    SERVICE_TASK,
    SCRIPT_TASK,
    BUSINESS_RULE_TASK,
    MANUAL_TASK,
    RECEIVE_TASK,
    SEND_TASK,
    CALL_ACTIVITY,
    SUB_PROCESS,
    TRANSACTION,
    AD_HOC_SUB_PROCESS,

    // ── Gateways ────────────────────────────────────────────────
    EXCLUSIVE_GATEWAY,
    INCLUSIVE_GATEWAY,
    PARALLEL_GATEWAY,
    EVENT_BASED_GATEWAY,
    COMPLEX_GATEWAY,

    // ── Sequence Flow ───────────────────────────────────────────
    SEQUENCE_FLOW,

    // ── Data ────────────────────────────────────────────────────
    DATA_OBJECT,
    DATA_OBJECT_REFERENCE,
    DATA_STORE_REFERENCE,
    DATA_INPUT,
    DATA_OUTPUT,

    // ── Artifacts ───────────────────────────────────────────────
    TEXT_ANNOTATION,
    ASSOCIATION,
    GROUP
}
