package com.bpmnengine.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity @Table(name = "hist_process_instance") @Data
public class HistProcessInstanceEntity {
    @Id private String id;
    @Column(name = "process_definition_id") private String processDefinitionId;
    @Column(name = "process_definition_key") private String processDefinitionKey;
    @Column(name = "process_definition_version") private Integer processDefinitionVersion;
    @Column(name = "business_key")  private String businessKey;
    @Column(name = "parent_instance_id") private String parentInstanceId;
    @Column(nullable = false)       private String state;
    @Column(name = "start_time")    private Instant startTime;
    @Column(name = "end_time")      private Instant endTime;
    @Column(name = "duration_ms")   private Long durationMs;
    @Column(name = "start_user_id") private String startUserId;
    @Column(name = "delete_reason") private String deleteReason;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
