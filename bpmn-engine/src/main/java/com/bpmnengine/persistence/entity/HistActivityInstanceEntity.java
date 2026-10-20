package com.bpmnengine.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity @Table(name = "hist_activity_instance") @Data
public class HistActivityInstanceEntity {
    @Id private String id;
    @Column(name = "activity_id", nullable = false)    private String activityId;
    @Column(name = "activity_name")                    private String activityName;
    @Column(name = "activity_type")                    private String activityType;
    @Column(name = "execution_id", nullable = false)   private String executionId;
    @Column(name = "process_instance_id", nullable = false) private String processInstanceId;
    @Column(name = "process_definition_id", nullable = false) private String processDefinitionId;
    @Column(name = "process_definition_key")           private String processDefinitionKey;
    @Column(name = "start_time")                       private Instant startTime;
    @Column(name = "end_time")                         private Instant endTime;
    @Column(name = "duration_ms")                      private Long durationMs;
    private String assignee;
    @Column(name = "task_id")                          private String taskId;
    @Column(name = "called_process_instance_id")       private String calledProcessInstanceId;
    @Column(name = "created_at", nullable = false)     private Instant createdAt;
}
