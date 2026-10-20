package com.bpmnengine.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity @Table(name = "process_instance")
@Data
public class ProcessInstanceEntity {

    public enum State { RUNNING, SUSPENDED, COMPLETED, TERMINATED, INCIDENT }

    @Id private String id;
    @Column(name = "business_key")             private String businessKey;
    @Column(name = "process_definition_id", nullable = false) private String processDefinitionId;
    @Column(name = "process_definition_key", nullable = false) private String processDefinitionKey;
    @Column(name = "parent_instance_id")       private String parentInstanceId;
    @Column(name = "super_execution_id")       private String superExecutionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false) private State state = State.RUNNING;

    @Column(name = "start_time", nullable = false)  private Instant startTime;
    @Column(name = "end_time")                      private Instant endTime;
    @Column(name = "duration_ms")                   private Long durationMs;
    @Column(name = "start_user_id")                 private String startUserId;
    @Column(name = "delete_reason")                 private String deleteReason;
    @Column(name = "created_at", nullable = false)  private Instant createdAt;
    @Column(name = "updated_at", nullable = false)  private Instant updatedAt;
}
