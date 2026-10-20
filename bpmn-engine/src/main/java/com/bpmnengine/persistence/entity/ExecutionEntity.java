package com.bpmnengine.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity @Table(name = "execution")
@Data
public class ExecutionEntity {

    public enum State { ACTIVE, WAITING, ENDED }

    @Id private String id;
    @Column(name = "process_instance_id", nullable = false) private String processInstanceId;
    @Column(name = "parent_execution_id")  private String parentExecutionId;
    @Column(name = "current_flow_node_id") private String currentFlowNodeId;
    @Column(name = "is_scope")       private Boolean isScope       = false;
    @Column(name = "is_concurrent")  private Boolean isConcurrent  = false;
    @Column(name = "is_active")      private Boolean isActive      = true;
    @Column(name = "is_event_scope") private Boolean isEventScope  = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false) private State state = State.ACTIVE;

    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
}
