package com.bpmnengine.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity @Table(name = "job")
@Data
public class JobEntity {

    @Id private String id;
    @Column(name = "job_type", nullable = false) private String type;
    @Column(name = "execution_id", nullable = false)      private String executionId;
    @Column(name = "process_instance_id", nullable = false) private String processInstanceId;
    @Column(name = "process_definition_id", nullable = false) private String processDefinitionId;
    @Column(name = "handler_type")   private String handlerType;
    @Column(name = "handler_config", columnDefinition = "TEXT") private String handlerConfig;
    private Instant duedate;
    @Column(name = "repeat_pattern") private String repeatPattern;
    private int retries = 3;
    @Column(name = "exception_message", columnDefinition = "TEXT") private String exceptionMessage;
    @Column(name = "exception_stacktrace", columnDefinition = "TEXT") private String exceptionStacktrace;
    private boolean suspended = false;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
