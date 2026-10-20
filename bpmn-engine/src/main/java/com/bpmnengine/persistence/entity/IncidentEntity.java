package com.bpmnengine.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity @Table(name = "incident") @Data
public class IncidentEntity {
    @Id private String id;
    @Column(name = "incident_type", nullable = false) private String incidentType;
    @Column(name = "execution_id", nullable = false)  private String executionId;
    @Column(name = "activity_id")                     private String activityId;
    @Column(name = "process_instance_id", nullable = false) private String processInstanceId;
    @Column(name = "process_definition_id", nullable = false) private String processDefinitionId;
    @Column(name = "cause_incident_id")               private String causeIncidentId;
    @Column(name = "root_cause_incident_id")          private String rootCauseIncidentId;
    @Column(columnDefinition = "TEXT")                private String configuration;
    @Column(name = "incident_message", columnDefinition = "TEXT") private String incidentMessage;
    @Column(nullable = false)                         private String state = "OPEN";
    @Column(name = "created_at", nullable = false)    private Instant createdAt;
    @Column(name = "resolved_at")                     private Instant resolvedAt;
}
