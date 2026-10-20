package com.bpmnengine.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity @Table(name = "event_subscription")
@Data
public class EventSubscriptionEntity {

    @Id private String id;
    @Column(name = "event_type", nullable = false)  private String eventType;
    @Column(name = "event_name")                    private String eventName;
    @Column(name = "execution_id", nullable = false) private String executionId;
    @Column(name = "process_instance_id")           private String processInstanceId;
    @Column(name = "process_definition_id")         private String processDefinitionId;
    @Column(name = "activity_id")                   private String activityId;
    @Column(columnDefinition = "TEXT")              private String configuration;
    @Column(name = "created_at", nullable = false)  private Instant createdAt;
}
