package com.bpmnengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * BPMN 2.0 Process Engine — Spring Boot entry point.
 *
 * Features:
 *  – Full BPMN 2.0 XML parsing (all element types)
 *  – Token-based execution engine with parallel/inclusive/exclusive/event-based gateways
 *  – UserTask, ServiceTask, ScriptTask, BusinessRuleTask, ReceiveTask, SendTask
 *  – SubProcess, CallActivity, Transactions
 *  – All event types: Start/End/Boundary/IntermediateCatch/IntermediateThrow
 *  – All event definitions: Timer, Message, Signal, Error, Escalation, Compensate, Conditional, Terminate
 *  – JUEL expression language for conditions and assignments
 *  – Quartz-powered timer support (ISO 8601 date/duration/cycle and cron)
 *  – JPA/Hibernate DB persistence with Flyway migrations (H2 dev, PostgreSQL prod)
 *  – Full audit history (process instance, activity instance, variables)
 *  – REST API: deploy, start, complete tasks, correlate messages, broadcast signals
 */
@SpringBootApplication
@EnableScheduling
public class BpmnEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(BpmnEngineApplication.class, args);
    }
}
