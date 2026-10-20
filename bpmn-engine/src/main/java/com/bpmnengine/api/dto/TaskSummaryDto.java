package com.bpmnengine.api.dto;

import com.bpmnengine.model.*;
import java.time.Instant;

/**
 * Lightweight task projection returned by GET /api/processes/{instanceId}/tasks.
 * Covers all BPMN task types (UserTask, ServiceTask, ScriptTask, SendTask,
 * ManualTask, BusinessRuleTask, generic Task) with a synthetic PENDING state for
 * elements not yet instantiated (e.g. sequential tasks in a parallel branch whose
 * predecessor hasn't completed yet).
 */
public class TaskSummaryDto {

    /** Task states — superset of TaskEntity.State with PENDING for not-yet-created tasks. */
    public enum State { CREATED, ASSIGNED, COMPLETED, DELETED, PENDING }

    private String  id;
    private String  name;
    private String  description;
    private String  taskDefinitionKey;
    private String  processInstanceId;
    private String  processDefinitionId;
    private String  executionId;
    private String  assignee;
    private String  owner;
    private String  candidateUsers;
    private String  candidateGroups;
    private State   state;
    private int     priority = 50;
    private String  formKey;
    private String  activityType;   // e.g. "USER_TASK", "SERVICE_TASK", "SCRIPT_TASK"
    private Instant dueDate;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;

    // ── Constructors ──────────────────────────────────────────────────────────

    public TaskSummaryDto() {}

    /** Build from a persisted TaskEntity (always a UserTask at the DB level). */
    public static TaskSummaryDto fromEntity(com.bpmnengine.persistence.entity.TaskEntity t) {
        TaskSummaryDto dto = new TaskSummaryDto();
        dto.id                = t.getId();
        dto.name              = t.getName();
        dto.description       = t.getDescription();
        dto.taskDefinitionKey = t.getTaskDefinitionKey();
        dto.processInstanceId = t.getProcessInstanceId();
        dto.processDefinitionId = t.getProcessDefinitionId();
        dto.executionId       = t.getExecutionId();
        dto.assignee          = t.getAssignee();
        dto.owner             = t.getOwner();
        dto.candidateUsers    = t.getCandidateUsers();
        dto.candidateGroups   = t.getCandidateGroups();
        dto.state             = State.valueOf(t.getState().name());
        dto.priority          = t.getPriority();
        dto.formKey           = t.getFormKey();
        dto.activityType      = "USER_TASK";
        dto.dueDate           = t.getDueDate();
        dto.createdAt         = t.getCreatedAt();
        dto.updatedAt         = t.getUpdatedAt();
        dto.completedAt       = t.getCompletedAt();
        return dto;
    }

    /**
     * Build a synthetic PENDING entry from any BPMN Activity model element.
     * Works for UserTask, ServiceTask, ScriptTask, SendTask, ManualTask,
     * BusinessRuleTask, and generic Task.
     */
    public static TaskSummaryDto pending(String processInstanceId,
                                         String processDefinitionId,
                                         Activity activity) {
        TaskSummaryDto dto = new TaskSummaryDto();
        dto.id                = null;    // not yet in DB
        dto.name              = activity.getName();
        dto.taskDefinitionKey = activity.getId();
        dto.processInstanceId = processInstanceId;
        dto.processDefinitionId = processDefinitionId;
        dto.state             = State.PENDING;
        dto.activityType      = activity.getElementType() != null
                                    ? activity.getElementType().name()
                                    : "TASK";

        // UserTask-specific fields
        if (activity instanceof UserTask ut) {
            dto.assignee        = ut.getAssignee();
            dto.candidateGroups = ut.getCandidateGroups().isEmpty()
                                      ? null : String.join(",", ut.getCandidateGroups());
            dto.candidateUsers  = ut.getCandidateUsers().isEmpty()
                                      ? null : String.join(",", ut.getCandidateUsers());
            dto.priority        = ut.getPriority();
            dto.formKey         = ut.getFormKey();
        }
        return dto;
    }

    // ── Getters / setters ─────────────────────────────────────────────────────

    public String  getId()                { return id; }
    public void    setId(String id)       { this.id = id; }
    public String  getName()              { return name; }
    public void    setName(String n)      { this.name = n; }
    public String  getDescription()       { return description; }
    public void    setDescription(String d){ this.description = d; }
    public String  getTaskDefinitionKey() { return taskDefinitionKey; }
    public void    setTaskDefinitionKey(String k){ this.taskDefinitionKey = k; }
    public String  getProcessInstanceId() { return processInstanceId; }
    public void    setProcessInstanceId(String p){ this.processInstanceId = p; }
    public String  getProcessDefinitionId(){ return processDefinitionId; }
    public void    setProcessDefinitionId(String p){ this.processDefinitionId = p; }
    public String  getExecutionId()       { return executionId; }
    public void    setExecutionId(String e){ this.executionId = e; }
    public String  getAssignee()          { return assignee; }
    public void    setAssignee(String a)  { this.assignee = a; }
    public String  getOwner()             { return owner; }
    public void    setOwner(String o)     { this.owner = o; }
    public String  getCandidateUsers()    { return candidateUsers; }
    public void    setCandidateUsers(String u){ this.candidateUsers = u; }
    public String  getCandidateGroups()   { return candidateGroups; }
    public void    setCandidateGroups(String g){ this.candidateGroups = g; }
    public State   getState()             { return state; }
    public void    setState(State s)      { this.state = s; }
    public int     getPriority()          { return priority; }
    public void    setPriority(int p)     { this.priority = p; }
    public String  getFormKey()           { return formKey; }
    public void    setFormKey(String f)   { this.formKey = f; }
    public String  getActivityType()      { return activityType; }
    public void    setActivityType(String t){ this.activityType = t; }
    public Instant getDueDate()           { return dueDate; }
    public void    setDueDate(Instant d)  { this.dueDate = d; }
    public Instant getCreatedAt()         { return createdAt; }
    public void    setCreatedAt(Instant c){ this.createdAt = c; }
    public Instant getUpdatedAt()         { return updatedAt; }
    public void    setUpdatedAt(Instant u){ this.updatedAt = u; }
    public Instant getCompletedAt()       { return completedAt; }
    public void    setCompletedAt(Instant c){ this.completedAt = c; }
}
