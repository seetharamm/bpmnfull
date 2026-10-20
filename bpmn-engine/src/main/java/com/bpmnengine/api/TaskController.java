package com.bpmnengine.api;

import com.bpmnengine.api.dto.TaskDetailDto;
import com.bpmnengine.api.dto.TaskSummaryDto;
import com.bpmnengine.engine.ProcessEngine;
import com.bpmnengine.engine.VariableMapper;
import com.bpmnengine.model.*;
import com.bpmnengine.model.Process;
import com.bpmnengine.parser.BpmnXmlParser;
import com.bpmnengine.persistence.entity.*;
import com.bpmnengine.persistence.repository.*;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST API for task management.
 *
 * GET  /api/tasks                         — list all open tasks
 * GET  /api/tasks/{taskId}                — get task
 * POST /api/tasks/{taskId}/complete       — complete a task
 * POST /api/tasks/{taskId}/claim          — claim a task
 * GET  /api/tasks/assignee/{userId}       — tasks by assignee
 * GET  /api/processes/{instanceId}/tasks  — tasks for a process instance
 */
@RestController
@RequestMapping("/api")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    private final ProcessEngine                   engine;
    private final TaskEntityRepository            taskRepo;
    private final HistActivityInstanceRepository  histAiRepo;
    private final ProcessVariableRepository       variableRepo;
    private final ExecutionRepository             executionRepo;
    private final ProcessDefinitionRepository     definitionRepo;
    private final ProcessInstanceRepository       instanceRepo;
    private final BpmnXmlParser                   parser;

    public TaskController(ProcessEngine engine,
                          TaskEntityRepository taskRepo,
                          HistActivityInstanceRepository histAiRepo,
                          ProcessVariableRepository variableRepo,
                          ExecutionRepository executionRepo,
                          ProcessDefinitionRepository definitionRepo,
                          ProcessInstanceRepository instanceRepo,
                          BpmnXmlParser parser) {
        this.engine         = engine;
        this.taskRepo       = taskRepo;
        this.histAiRepo     = histAiRepo;
        this.variableRepo   = variableRepo;
        this.executionRepo  = executionRepo;
        this.definitionRepo = definitionRepo;
        this.instanceRepo   = instanceRepo;
        this.parser         = parser;
    }

    @GetMapping("/tasks")
    public List<TaskEntity> listOpenTasks() {
        return taskRepo.findByStateIn(
                Arrays.asList(TaskEntity.State.CREATED, TaskEntity.State.ASSIGNED));
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<TaskEntity> getTask(@PathVariable String taskId) {
        return taskRepo.findById(taskId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/tasks/{taskId}/complete")
    public ResponseEntity<Void> completeTask(@PathVariable String taskId,
                                              @RequestBody(required = false) Map<String, Object> variables) {
        engine.completeTask(taskId, variables != null ? variables : Map.of());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tasks/{taskId}/claim")
    public ResponseEntity<Void> claimTask(@PathVariable String taskId,
                                           @RequestParam String userId) {
        engine.claimTask(taskId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tasks/assignee/{userId}")
    public List<TaskEntity> tasksByAssignee(@PathVariable String userId) {
        return taskRepo.findByAssignee(userId);
    }

    @GetMapping("/processes/{instanceId}/tasks")
    public List<TaskSummaryDto> tasksForInstance(
            @PathVariable String instanceId,
            @RequestParam(required = false) String state) {

        // 1. Fetch all stored tasks for this instance
        List<TaskEntity> stored = taskRepo.findByProcessInstanceId(instanceId);
        log.info("tasksForInstance instanceId={} stored={} state-filter={}", instanceId, stored.size(), state);

        // 2. Look up the process definition to enrich with PENDING tasks
        ProcessInstanceEntity instance = instanceRepo.findById(instanceId).orElse(null);
        List<TaskSummaryDto> result = stored.stream()
                .map(TaskSummaryDto::fromEntity)
                .collect(Collectors.toList());

        if (instance != null) {
            // Keys already stored for this instance
            Set<String> storedKeys = stored.stream()
                    .map(TaskEntity::getTaskDefinitionKey)
                    .filter(k -> k != null)
                    .collect(Collectors.toSet());

            // Parse the BPMN model and collect ALL task elements (any Activity that is
            // not a SubProcess or CallActivity — those have their own execution mechanics).
            definitionRepo.findById(instance.getProcessDefinitionId()).ifPresent(def -> {
                try {
                    BpmnModel model = parser.parse(def.getBpmnXml());
                    List<Process> processes = model.getProcesses();
                    for (Process proc : processes) {
                        proc.getFlowElements().stream()
                                .filter(fe -> fe instanceof Activity
                                           && !(fe instanceof SubProcess)
                                           && !(fe instanceof CallActivity))
                                .map(fe -> (Activity) fe)
                                .filter(a -> !storedKeys.contains(a.getId()))
                                .map(a -> TaskSummaryDto.pending(instanceId, def.getId(), a))
                                .forEach(result::add);
                    }
                } catch (Exception e) {
                    log.warn("Could not enrich tasks with PENDING entries for instance {}: {}", instanceId, e.getMessage());
                }
            });
        }

        // 3. Optionally filter by state
        if (state != null && !state.isBlank()) {
            String upper = state.toUpperCase();
            return result.stream()
                    .filter(t -> t.getState().name().equals(upper))
                    .collect(Collectors.toList());
        }
        return result;
    }

    /**
     * Returns all history activity instances for a process instance.
     * Includes every node type (service tasks, script tasks, gateways, events, etc.),
     * not just user tasks. Used by the UI to colour the BPMN diagram.
     */
    @GetMapping("/processes/{instanceId}/activities")
    public List<HistActivityInstanceEntity> activitiesForInstance(@PathVariable String instanceId) {
        return histAiRepo.findByProcessInstanceId(instanceId);
    }
    /**
     * Returns full task detail: task fields, process variables, local variables,
     * and I/O mappings declared in the BPMN model.
     */
    @GetMapping("/tasks/{taskId}/detail")
    public ResponseEntity<TaskDetailDto> getTaskDetail(@PathVariable String taskId) {
        return taskRepo.findById(taskId)
                .map(task -> ResponseEntity.ok(buildDetail(task)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── private builder ───────────────────────────────────────────────────────

    private TaskDetailDto buildDetail(TaskEntity task) {
        TaskDetailDto dto = new TaskDetailDto();

        // Core fields
        dto.setId(task.getId());
        dto.setName(task.getName());
        dto.setDescription(task.getDescription());
        dto.setTaskDefinitionKey(task.getTaskDefinitionKey());
        dto.setProcessInstanceId(task.getProcessInstanceId());
        dto.setProcessDefinitionId(task.getProcessDefinitionId());
        dto.setExecutionId(task.getExecutionId());
        dto.setAssignee(task.getAssignee());
        dto.setOwner(task.getOwner());
        dto.setCandidateUsers(task.getCandidateUsers());
        dto.setCandidateGroups(task.getCandidateGroups());
        dto.setState(task.getState().name());
        dto.setPriority(task.getPriority());
        dto.setFormKey(task.getFormKey());
        dto.setDueDate(task.getDueDate());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setUpdatedAt(task.getUpdatedAt());
        dto.setCompletedAt(task.getCompletedAt());

        // Resolve processDefinitionKey from execution → instance or definition
        executionRepo.findById(task.getExecutionId()).ifPresent(exec -> {
            // Local (execution-scoped) variables
            variableRepo.findByExecutionId(exec.getId()).forEach(v ->
                    dto.getLocalVariables().add(toVariableDto(v)));
        });

        // Process-scope variables
        variableRepo.findByProcessInstanceId(task.getProcessInstanceId()).forEach(v ->
                dto.getProcessVariables().add(toVariableDto(v)));

        // I/O mappings from the BPMN model
        try {
            definitionRepo.findById(task.getProcessDefinitionId()).ifPresent(def -> {
                dto.setProcessDefinitionKey(def.getKey());
                BpmnModel model = parser.parse(def.getBpmnXml());
                Process process = model.getProcesses().isEmpty() ? null : model.getProcesses().get(0);
                if (process != null && task.getTaskDefinitionKey() != null) {
                    FlowElement fe = process.getFlowElement(task.getTaskDefinitionKey());
                    if (fe instanceof Activity act) {
                        // Data input associations  (variable → task input)
                        act.getDataInputAssociations().forEach(assoc -> {
                            TaskDetailDto.IoMappingDto m = new TaskDetailDto.IoMappingDto();
                            m.setSource(assoc.getSourceRefs().isEmpty() ? null : String.join(", ", assoc.getSourceRefs()));
                            m.setTarget(assoc.getTargetRef());
                            m.setTransformation(assoc.getTransformation());
                            dto.getInputs().add(m);
                        });
                        // Data output associations (task output → variable)
                        act.getDataOutputAssociations().forEach(assoc -> {
                            TaskDetailDto.IoMappingDto m = new TaskDetailDto.IoMappingDto();
                            m.setSource(assoc.getSourceRefs().isEmpty() ? null : String.join(", ", assoc.getSourceRefs()));
                            m.setTarget(assoc.getTargetRef());
                            m.setTransformation(assoc.getTransformation());
                            dto.getOutputs().add(m);
                        });
                        // IoSpecification data inputs/outputs (formal declarations)
                        IoSpecification ioSpec = act.getIoSpecification();
                        if (ioSpec != null) {
                            ioSpec.getDataInputs().forEach(di -> {
                                TaskDetailDto.IoMappingDto m = new TaskDetailDto.IoMappingDto();
                                m.setSource(di.getName() != null ? di.getName() : di.getId());
                                m.setTarget(di.getItemSubjectRef());
                                dto.getInputs().add(m);
                            });
                            ioSpec.getDataOutputs().forEach(dout -> {
                                TaskDetailDto.IoMappingDto m = new TaskDetailDto.IoMappingDto();
                                m.setSource(dout.getName() != null ? dout.getName() : dout.getId());
                                m.setTarget(dout.getItemSubjectRef());
                                dto.getOutputs().add(m);
                            });
                        }
                    }
                }
            });
        } catch (Exception e) {
            // Non-fatal: I/O spec is optional
        }

        return dto;
    }

    private TaskDetailDto.VariableDto toVariableDto(ProcessVariableEntity v) {
        TaskDetailDto.VariableDto d = new TaskDetailDto.VariableDto();
        d.setName(v.getName());
        d.setType(v.getType());
        d.setUpdatedAt(v.getUpdatedAt());
        Object raw = VariableMapper.mapFromEntity(v);
        d.setValue(raw == null ? "null" : raw.toString());
        return d;
    }
}
