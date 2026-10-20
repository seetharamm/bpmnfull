package com.bpmnengine.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity @Table(name = "task")
@Data
public class TaskEntity {

    public enum State { CREATED, ASSIGNED, COMPLETED, DELETED }

    @Id private String id;
    @Column(name = "task_name") private String name;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(name = "execution_id", nullable = false)           private String executionId;
    @Column(name = "process_instance_id", nullable = false)    private String processInstanceId;
    @Column(name = "process_definition_id", nullable = false)  private String processDefinitionId;
    @Column(name = "task_definition_key")  private String taskDefinitionKey;
    private String assignee;
    private String owner;
    @Column(name = "candidate_users")  private String candidateUsers;
    @Column(name = "candidate_groups") private String candidateGroups;
    @Column(name = "due_date")         private Instant dueDate;
    @Column(name = "follow_up_date")   private Instant followUpDate;
    private int priority = 50;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false) private State state = State.CREATED;

    @Column(name = "form_key")   private String formKey;
    private String category;
    @Column(name = "created_at",   nullable = false) private Instant createdAt;
    @Column(name = "updated_at",   nullable = false) private Instant updatedAt;
    @Column(name = "completed_at")                   private Instant completedAt;
}
