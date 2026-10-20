package com.bpmnengine.engine.handler;

import com.bpmnengine.engine.ExecutionContext;
import com.bpmnengine.engine.ExecutionOutcome;
import com.bpmnengine.identity.service.IdentityService;
import com.bpmnengine.model.*;
import com.bpmnengine.persistence.entity.TaskEntity;
import com.bpmnengine.persistence.repository.TaskEntityRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles &lt;userTask&gt; — creates a task record and enters a wait state.
 * Candidate groups are resolved to actual usernames via {@link IdentityService}.
 * Execution resumes when {@code ProcessEngine.completeTask()} is called.
 */
@Component
public class UserTaskHandler implements FlowElementHandler {

    private final TaskEntityRepository taskRepo;
    private final IdentityService      identityService;

    public UserTaskHandler(TaskEntityRepository taskRepo, IdentityService identityService) {
        this.taskRepo        = taskRepo;
        this.identityService = identityService;
    }

    @Override
    public boolean supports(FlowElement element) {
        return element instanceof UserTask;
    }

    @Override
    public ExecutionOutcome execute(ExecutionContext ctx, FlowElement element) {
        UserTask userTask = (UserTask) element;

        boolean alreadyCompleted = taskRepo
                .findByExecutionIdAndTaskDefinitionKey(ctx.getId(), userTask.getId())
                .map(t -> t.getState() == TaskEntity.State.COMPLETED)
                .orElse(false);

        if (alreadyCompleted) {
            List<SequenceFlow> flows = ctx.getProcess().getOutgoingFlows(userTask.getId());
            return flows.isEmpty() ? ExecutionOutcome.endProcess() : ExecutionOutcome.continueOn(flows.get(0));
        }

        // Create the task
        TaskEntity task = new TaskEntity();
        task.setId(UUID.randomUUID().toString());
        task.setExecutionId(ctx.getId());
        task.setProcessInstanceId(ctx.getProcessInstanceId());
        task.setProcessDefinitionId(ctx.getProcessDefinitionId());
        task.setTaskDefinitionKey(userTask.getId());
        task.setName(userTask.getName());
        task.setDescription(userTask.getDocumentation());
        task.setState(TaskEntity.State.CREATED);
        task.setPriority(userTask.getPriority());
        task.setFormKey(userTask.getFormKey());
        task.setCreatedAt(Instant.now());
        task.setUpdatedAt(Instant.now());

        // Resolve assignee
        String assignee = userTask.getAssignee();
        if (assignee != null && !assignee.isBlank()) {
            task.setAssignee(assignee);
            task.setState(TaskEntity.State.ASSIGNED);
        }

        // Candidate users (direct)
        if (!userTask.getCandidateUsers().isEmpty()) {
            task.setCandidateUsers(String.join(",", userTask.getCandidateUsers()));
        }

        // Candidate groups — store the group names AND resolve to usernames via identity store
        if (!userTask.getCandidateGroups().isEmpty()) {
            task.setCandidateGroups(String.join(",", userTask.getCandidateGroups()));

            // Resolve group members and add them as candidate users too
            List<String> resolvedUsers = userTask.getCandidateGroups().stream()
                    .flatMap(gn -> identityService.getUsernamesInGroup(gn).stream())
                    .distinct()
                    .collect(Collectors.toList());

            if (!resolvedUsers.isEmpty()) {
                // Merge with existing candidateUsers
                String existing = task.getCandidateUsers();
                String merged = (existing != null && !existing.isBlank())
                        ? existing + "," + String.join(",", resolvedUsers)
                        : String.join(",", resolvedUsers);
                task.setCandidateUsers(merged);
            }
        }

        taskRepo.save(task);
        return ExecutionOutcome.waitState();
    }
}
