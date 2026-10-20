package com.bpmnengine.engine;

import com.bpmnengine.api.dto.*;
import com.bpmnengine.engine.handler.*;
import com.bpmnengine.engine.expression.ExpressionEvaluator;
import com.bpmnengine.model.*;
import com.bpmnengine.model.Process;
import com.bpmnengine.persistence.entity.*;
import com.bpmnengine.persistence.repository.*;
import com.bpmnengine.parser.BpmnXmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core process execution engine.
 *
 * Responsibilities:
 *  1. Deploy BPMN XML → persist ProcessDefinitionEntity
 *  2. Instantiate processes → create ProcessInstanceEntity + root ExecutionEntity
 *  3. Execute flow elements in a loop (agenda-based)
 *  4. Persist all state changes
 *  5. Delegate element-specific logic to {@link FlowElementHandler} implementations
 */
@Service
@Transactional
public class ProcessEngineImpl implements ProcessEngine {

    private static final Logger log = LoggerFactory.getLogger(ProcessEngineImpl.class);

    private final BpmnXmlParser                   parser;
    private final ProcessDefinitionRepository     definitionRepo;
    private final ProcessInstanceRepository       instanceRepo;
    private final ExecutionRepository             executionRepo;
    private final ProcessVariableRepository       variableRepo;
    private final TaskEntityRepository            taskRepo;
    private final JobRepository                   jobRepo;
    private final EventSubscriptionRepository     subscriptionRepo;
    private final HistoryService                  historyService;
    private final FlowElementHandlerRegistry      handlerRegistry;
    private final ExpressionEvaluator             expressionEvaluator;

    // In-memory cache of parsed models, keyed by definition id
    private final Map<String, BpmnModel> modelCache = new HashMap<>();

    public ProcessEngineImpl(BpmnXmlParser parser,
                             ProcessDefinitionRepository definitionRepo,
                             ProcessInstanceRepository instanceRepo,
                             ExecutionRepository executionRepo,
                             ProcessVariableRepository variableRepo,
                             TaskEntityRepository taskRepo,
                             JobRepository jobRepo,
                             EventSubscriptionRepository subscriptionRepo,
                             HistoryService historyService,
                             FlowElementHandlerRegistry handlerRegistry,
                             ExpressionEvaluator expressionEvaluator) {
        this.parser              = parser;
        this.definitionRepo      = definitionRepo;
        this.instanceRepo        = instanceRepo;
        this.executionRepo       = executionRepo;
        this.variableRepo        = variableRepo;
        this.taskRepo            = taskRepo;
        this.jobRepo             = jobRepo;
        this.subscriptionRepo    = subscriptionRepo;
        this.historyService      = historyService;
        this.handlerRegistry     = handlerRegistry;
        this.expressionEvaluator = expressionEvaluator;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Deployment
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public ProcessDefinitionDto deployProcess(DeployProcessRequest request) {
        BpmnModel model = parser.parse(request.getBpmnXml());

        if (model.getProcesses().isEmpty()) {
            throw new EngineException("No process found in BPMN XML");
        }

        Process process = model.getProcesses().get(0);
        String key = process.getId();

        int nextVersion = definitionRepo.findLatestVersionByKey(key)
                .map(d -> d.getVersion() + 1)
                .orElse(1);

        // Deactivate previous active version for this key
        definitionRepo.clearActiveForKey(key);

        ProcessDefinitionEntity def = new ProcessDefinitionEntity();
        def.setId(UUID.randomUUID().toString());
        def.setKey(key);
        def.setVersion(nextVersion);
        def.setName(process.getName());
        def.setBpmnXml(request.getBpmnXml());
        def.setResourceName(request.getResourceName() != null
                ? request.getResourceName() : key + ".bpmn20.xml");
        def.setDeployedBy(request.getDeployedBy());
        def.setDeployMessage(request.getDeployMessage());
        def.setActive(true);
        def.setSuspended(false);
        def.setCreatedAt(Instant.now());
        def.setUpdatedAt(Instant.now());
        definitionRepo.save(def);

        modelCache.put(def.getId(), model);

        log.info("Deployed process '{}' version {} → id={}", key, nextVersion, def.getId());
        return toDefinitionDto(def, false);
    }

    @Override
    public String deployProcess(String resourceName, String bpmnXml) {
        DeployProcessRequest req = new DeployProcessRequest();
        req.setResourceName(resourceName);
        req.setBpmnXml(bpmnXml);
        return deployProcess(req).getId();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Definition queries
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ProcessDefinitionDto> listDefinitions() {
        return definitionRepo.findAllActiveDefinitions().stream()
                .map(d -> toDefinitionDto(d, false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcessDefinitionDto> searchDefinitions(String query) {
        return definitionRepo.searchActive(query).stream()
                .map(d -> toDefinitionDto(d, false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProcessDefinitionDto getActiveDefinition(String processKey) {
        ProcessDefinitionEntity def = definitionRepo.findActiveByKey(processKey)
                .orElseThrow(() -> new EngineException("No active definition for key: " + processKey));
        return toDefinitionDto(def, true);
    }

    @Override
    @Transactional(readOnly = true)
    public ProcessDefinitionDto getDefinitionByVersion(String processKey, int version) {
        ProcessDefinitionEntity def = definitionRepo.findByKeyAndVersion(processKey, version)
                .orElseThrow(() -> new EngineException(
                        "Process definition not found: " + processKey + " v" + version));
        return toDefinitionDto(def, true);
    }

    @Override
    @Transactional(readOnly = true)
    public ProcessDefinitionDto getDefinitionById(String definitionId) {
        ProcessDefinitionEntity def = definitionRepo.findById(definitionId)
                .orElseThrow(() -> new EngineException("Process definition not found: " + definitionId));
        return toDefinitionDto(def, true);
    }

    @Override
    @Transactional(readOnly = true)
    public ProcessVersionSummaryDto getVersionHistory(String processKey) {
        List<ProcessDefinitionEntity> versions = definitionRepo.findAllVersionsByKey(processKey);
        if (versions.isEmpty()) {
            throw new EngineException("No process definition found for key: " + processKey);
        }

        ProcessDefinitionEntity latest = versions.get(0);
        int activeVersion = versions.stream()
                .filter(ProcessDefinitionEntity::isActive)
                .mapToInt(ProcessDefinitionEntity::getVersion)
                .findFirst().orElse(latest.getVersion());

        long totalRunning = instanceRepo.countRunningByKey(processKey);

        ProcessVersionSummaryDto summary = new ProcessVersionSummaryDto();
        summary.setKey(processKey);
        summary.setName(latest.getName());
        summary.setLatestVersion(latest.getVersion());
        summary.setActiveVersion(activeVersion);
        summary.setSuspended(versions.stream().allMatch(ProcessDefinitionEntity::isSuspended));
        summary.setTotalVersions(versions.size());
        summary.setTotalRunningInstances(totalRunning);
        summary.setVersions(versions.stream()
                .map(d -> toDefinitionDto(d, false))
                .collect(Collectors.toList()));
        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public String getBpmnXml(String processKey, int version) {
        return definitionRepo.findByKeyAndVersion(processKey, version)
                .orElseThrow(() -> new EngineException(
                        "Process definition not found: " + processKey + " v" + version))
                .getBpmnXml();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Version management
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public ProcessDefinitionDto setActiveVersion(String processKey, int version) {
        definitionRepo.findByKeyAndVersion(processKey, version)
                .orElseThrow(() -> new EngineException(
                        "Process definition not found: " + processKey + " v" + version));
        definitionRepo.setActiveVersion(processKey, version);
        return toDefinitionDto(
                definitionRepo.findByKeyAndVersion(processKey, version).orElseThrow(), false);
    }

    @Override
    public void deleteVersion(String processKey, int version) {
        ProcessDefinitionEntity def = definitionRepo.findByKeyAndVersion(processKey, version)
                .orElseThrow(() -> new EngineException(
                        "Process definition not found: " + processKey + " v" + version));

        long running = instanceRepo.countByProcessDefinitionIdAndState(
                def.getId(), ProcessInstanceEntity.State.RUNNING);
        if (running > 0) {
            throw new EngineException("Cannot delete version " + version + " of '" + processKey +
                    "': " + running + " running instance(s)");
        }

        // When deleting the active version, make the next newest version active
        if (def.isActive()) {
            List<ProcessDefinitionEntity> others = definitionRepo.findAllVersionsByKey(processKey)
                    .stream()
                    .filter(d -> d.getVersion() != version)
                    .collect(Collectors.toList());
            if (!others.isEmpty()) {
                ProcessDefinitionEntity newActive = others.get(0); // newest first
                definitionRepo.setActiveVersion(processKey, newActive.getVersion());
            }
        }

        modelCache.remove(def.getId());
        definitionRepo.delete(def);
        log.info("Deleted process '{}' v{}", processKey, version);
    }

    @Override
    public void deleteAllVersions(String processKey) {
        long running = instanceRepo.countRunningByKey(processKey);
        if (running > 0) {
            throw new EngineException("Cannot delete process '" + processKey +
                    "': " + running + " running instance(s)");
        }
        List<ProcessDefinitionEntity> all = definitionRepo.findAllVersionsByKey(processKey);
        all.forEach(d -> modelCache.remove(d.getId()));
        definitionRepo.deleteAll(all);
        log.info("Deleted all versions of process '{}'", processKey);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Suspend / activate definitions
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public void suspendProcessDefinition(String processKey) {
        ensureKeyExists(processKey);
        definitionRepo.setSuspendedForKey(processKey, true);
        log.info("Suspended all versions of process '{}'", processKey);
    }

    @Override
    public void activateProcessDefinition(String processKey) {
        ensureKeyExists(processKey);
        definitionRepo.setSuspendedForKey(processKey, false);
        log.info("Activated all versions of process '{}'", processKey);
    }

    @Override
    public void suspendProcessDefinitionVersion(String processKey, int version) {
        ProcessDefinitionEntity def = definitionRepo.findByKeyAndVersion(processKey, version)
                .orElseThrow(() -> new EngineException(
                        "Process definition not found: " + processKey + " v" + version));
        def.setSuspended(true);
        def.setUpdatedAt(Instant.now());
        definitionRepo.save(def);
    }

    @Override
    public void activateProcessDefinitionVersion(String processKey, int version) {
        ProcessDefinitionEntity def = definitionRepo.findByKeyAndVersion(processKey, version)
                .orElseThrow(() -> new EngineException(
                        "Process definition not found: " + processKey + " v" + version));
        def.setSuspended(false);
        def.setUpdatedAt(Instant.now());
        definitionRepo.save(def);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Start Process
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public ProcessInstanceResult startProcessByKey(String processKey, Map<String, Object> variables) {
        return startProcessByKey(processKey, null, variables);
    }

    @Override
    public ProcessInstanceResult startProcessByKey(String processKey, String businessKey,
                                                    Map<String, Object> variables) {
        ProcessDefinitionEntity def = definitionRepo.findActiveByKey(processKey)
                .orElseGet(() -> definitionRepo.findLatestByKey(processKey)
                        .orElseThrow(() -> new EngineException("Process not found: " + processKey)));
        guardNotSuspended(def);

        return startFromDefinition(def, businessKey, variables);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Execution Loop (Agenda)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Execute the element at ctx.currentFlowElement and follow the resulting
     * execution agenda until the process is waiting or complete.
     */
    public void executeElement(ExecutionContext ctx) {
        while (ctx.isActive() && !ctx.isEnded()) {
            FlowElement current = ctx.getCurrentFlowElement();
            if (current == null) {
                log.warn("Execution {} has no current element — ending", ctx.getId());
                ctx.end();
                break;
            }

            log.debug("Executing [{}] {} (exec={})", current.getElementType(), current.getId(), ctx.getId());

            // Record history
            historyService.recordActivityStarted(ctx, current);

            // Dispatch to handler
            FlowElementHandler handler = handlerRegistry.getHandler(current);
            if (handler == null) {
                throw new EngineException("No handler for element type: " + current.getElementType());
            }

            ExecutionOutcome outcome = handler.execute(ctx, current);

            // Only record activity end when the element actually finished (not a wait state).
            // UserTask / ReceiveTask return waitState() — they will be ended when resumed.
            boolean isWait = outcome == null || outcome.isWait();
            if (!isWait) {
                historyService.recordActivityEnded(ctx, current);
            }

            if (outcome != null && outcome.isEnd()) {
                // Persist the ended state BEFORE checking activeCount, so the
                // countActiveByProcessInstanceId query sees this execution as inactive.
                persistExecutionState(ctx);
                handleProcessEnd(ctx);
                break;
            }

            // Persist execution state
            persistExecutionState(ctx);

            if (isWait) {
                // Handler signalled that execution should pause (wait state)
                break;
            }

            // Follow the outgoing sequence flows
            if (outcome.getOutgoingFlows().isEmpty()) {
                log.warn("No outgoing flows from element {} — ending execution", current.getId());
                handleProcessEnd(ctx);
                break;
            }

            // Single token: take the first flow (already chosen by handler)
            SequenceFlow nextFlow = outcome.getOutgoingFlows().get(0);
            takeSequenceFlow(ctx, nextFlow);

            // Spawn parallel branches and execute each one immediately.
            // Without the executeElement call the branch sits in the DB with its
            // currentFlowNodeId set but the task/history is never created.
            for (int i = 1; i < outcome.getOutgoingFlows().size(); i++) {
                ExecutionContext branch = forkExecution(ctx);
                takeSequenceFlow(branch, outcome.getOutgoingFlows().get(i));
                executeElement(branch);   // ← run the branch; stops at wait state or end
            }
        }
    }

    private void takeSequenceFlow(ExecutionContext ctx, SequenceFlow sf) {
        log.debug("Taking sequence flow {} → {}", sf.getId(), sf.getTargetRef());
        historyService.recordSequenceFlowTaken(ctx, sf);

        FlowElement target = ctx.getProcess().getFlowElement(sf.getTargetRef());
        if (target == null) throw new EngineException("Target element not found: " + sf.getTargetRef());

        ctx.setCurrentFlowElement(target);

        // Persist
        ExecutionEntity exec = executionRepo.findById(ctx.getId())
                .orElseThrow(() -> new EngineException("Execution not found: " + ctx.getId()));
        exec.setCurrentFlowNodeId(target.getId());
        exec.setUpdatedAt(Instant.now());
        executionRepo.save(exec);
    }

    private void handleProcessEnd(ExecutionContext ctx) {
        ctx.end();
        ProcessInstanceEntity instance = instanceRepo.findById(ctx.getProcessInstanceId())
                .orElseThrow();
        // Check if all root executions are ended
        long activeCount = executionRepo.countActiveByProcessInstanceId(ctx.getProcessInstanceId());
        if (activeCount == 0) {
            instance.setState(ProcessInstanceEntity.State.COMPLETED);
            instance.setEndTime(Instant.now());
            instance.setDurationMs(
                    instance.getEndTime().toEpochMilli() - instance.getStartTime().toEpochMilli()
            );
            instance.setUpdatedAt(Instant.now());
            instanceRepo.save(instance);
            historyService.recordProcessEnded(instance);
            log.info("Process instance {} completed", instance.getId());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Message / Signal correlation
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public void correlateMessage(String messageName, String businessKey, Map<String, Object> variables) {
        List<EventSubscriptionEntity> subs = subscriptionRepo
                .findByEventTypeAndEventName("MESSAGE", messageName);

        if (subs.isEmpty()) throw new EngineException("No subscriber for message: " + messageName);

        for (EventSubscriptionEntity sub : subs) {
            ExecutionEntity exec = executionRepo.findById(sub.getExecutionId()).orElseThrow();
            ProcessInstanceEntity instance = instanceRepo.findById(exec.getProcessInstanceId()).orElseThrow();

            if (businessKey != null && !businessKey.equals(instance.getBusinessKey())) continue;

            ProcessDefinitionEntity def = definitionRepo.findById(instance.getProcessDefinitionId()).orElseThrow();
            BpmnModel model = getOrParseModel(def);
            Process process = model.getProcesses().get(0);

            ExecutionContext ctx = buildExecutionContext(exec, instance, model, process,
                    loadVariables(instance.getId()));
            if (variables != null) variables.forEach(ctx::setVariable);

            subscriptionRepo.delete(sub);

            // Resume from the waiting element
            FlowElement current = process.getFlowElement(exec.getCurrentFlowNodeId());
            ctx.setCurrentFlowElement(current);

            // The element handler will take over from here
            executeElement(ctx);
        }
    }

    @Override
    public void signalAll(String signalName, Map<String, Object> variables) {
        List<EventSubscriptionEntity> subs = subscriptionRepo
                .findByEventTypeAndEventName("SIGNAL", signalName);
        for (EventSubscriptionEntity sub : subs) {
            signalSubscription(sub, variables);
        }
    }

    @Override
    public void signalExecution(String executionId, Map<String, Object> variables) {
        ExecutionEntity exec = executionRepo.findById(executionId).orElseThrow();
        ProcessDefinitionEntity def = definitionRepo.findById(
                instanceRepo.findById(exec.getProcessInstanceId()).orElseThrow().getProcessDefinitionId()
        ).orElseThrow();
        BpmnModel model = getOrParseModel(def);
        Process process = model.getProcesses().get(0);
        ProcessInstanceEntity instance = instanceRepo.findById(exec.getProcessInstanceId()).orElseThrow();

        ExecutionContext ctx = buildExecutionContext(exec, instance, model, process, loadVariables(instance.getId()));
        if (variables != null) variables.forEach(ctx::setVariable);

        FlowElement current = process.getFlowElement(exec.getCurrentFlowNodeId());
        ctx.setCurrentFlowElement(current);
        executeElement(ctx);
    }

    private void signalSubscription(EventSubscriptionEntity sub, Map<String, Object> variables) {
        ExecutionEntity exec = executionRepo.findById(sub.getExecutionId()).orElseThrow();
        ProcessInstanceEntity instance = instanceRepo.findById(exec.getProcessInstanceId()).orElseThrow();
        ProcessDefinitionEntity def = definitionRepo.findById(instance.getProcessDefinitionId()).orElseThrow();
        BpmnModel model = getOrParseModel(def);
        Process process = model.getProcesses().get(0);
        ExecutionContext ctx = buildExecutionContext(exec, instance, model, process, loadVariables(instance.getId()));
        if (variables != null) variables.forEach(ctx::setVariable);
        subscriptionRepo.delete(sub);
        FlowElement current = process.getFlowElement(exec.getCurrentFlowNodeId());
        ctx.setCurrentFlowElement(current);
        executeElement(ctx);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Task operations
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public void completeTask(String taskId, Map<String, Object> variables) {
        TaskEntity task = taskRepo.findById(taskId)
                .orElseThrow(() -> new EngineException("Task not found: " + taskId));
        if (task.getState() != TaskEntity.State.CREATED && task.getState() != TaskEntity.State.ASSIGNED) {
            throw new EngineException("Task is not in a completable state: " + task.getState());
        }

        task.setState(TaskEntity.State.COMPLETED);
        task.setCompletedAt(Instant.now());
        task.setUpdatedAt(Instant.now());
        taskRepo.save(task);

        ExecutionEntity exec = executionRepo.findById(task.getExecutionId()).orElseThrow();
        ProcessInstanceEntity instance = instanceRepo.findById(exec.getProcessInstanceId()).orElseThrow();
        ProcessDefinitionEntity def = definitionRepo.findById(instance.getProcessDefinitionId()).orElseThrow();
        BpmnModel model = getOrParseModel(def);
        Process process = model.getProcesses().get(0);

        ExecutionContext ctx = buildExecutionContext(exec, instance, model, process, loadVariables(instance.getId()));
        if (variables != null) variables.forEach(ctx::setVariable);
        if (variables != null) variables.forEach((k, v) -> persistVariable(instance.getId(), exec.getId(), k, v));

        FlowElement current = process.getFlowElement(exec.getCurrentFlowNodeId());
        ctx.setCurrentFlowElement(current);

        // Execute (the UserTask handler will take the outgoing flow when it sees the task is complete)
        executeFromAfter(ctx, current);
    }

    @Override
    public void claimTask(String taskId, String userId) {
        TaskEntity task = taskRepo.findById(taskId)
                .orElseThrow(() -> new EngineException("Task not found: " + taskId));
        task.setAssignee(userId);
        task.setState(TaskEntity.State.ASSIGNED);
        task.setUpdatedAt(Instant.now());
        taskRepo.save(task);
    }

    @Override
    public void triggerTimerJob(String jobId) {
        JobEntity job = jobRepo.findById(jobId)
                .orElseThrow(() -> new EngineException("Job not found: " + jobId));
        ExecutionEntity exec = executionRepo.findById(job.getExecutionId()).orElseThrow();
        ProcessInstanceEntity instance = instanceRepo.findById(exec.getProcessInstanceId()).orElseThrow();
        ProcessDefinitionEntity def = definitionRepo.findById(instance.getProcessDefinitionId()).orElseThrow();
        BpmnModel model = getOrParseModel(def);
        Process process = model.getProcesses().get(0);

        ExecutionContext ctx = buildExecutionContext(exec, instance, model, process, loadVariables(instance.getId()));
        jobRepo.delete(job);

        FlowElement current = process.getFlowElement(exec.getCurrentFlowNodeId());
        ctx.setCurrentFlowElement(current);
        executeFromAfter(ctx, current);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Process instance management
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ProcessInstanceResult> listInstances(String state) {
        List<ProcessInstanceEntity> entities = (state != null && !state.isBlank())
                ? instanceRepo.findByState(ProcessInstanceEntity.State.valueOf(state.toUpperCase()))
                : instanceRepo.findAll();
        return entities.stream().map(this::toResult).collect(Collectors.toList());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ProcessInstanceResult getProcessInstance(String processInstanceId) {
        ProcessInstanceEntity instance = instanceRepo.findById(processInstanceId)
                .orElseThrow(() -> new EngineException("Process instance not found: " + processInstanceId));
        return toResult(instance);
    }

    @Override
    public void suspendProcessInstance(String processInstanceId) {
        ProcessInstanceEntity instance = instanceRepo.findById(processInstanceId).orElseThrow();
        instance.setState(ProcessInstanceEntity.State.SUSPENDED);
        instance.setUpdatedAt(Instant.now());
        instanceRepo.save(instance);
    }

    @Override
    public void activateProcessInstance(String processInstanceId) {
        ProcessInstanceEntity instance = instanceRepo.findById(processInstanceId).orElseThrow();
        instance.setState(ProcessInstanceEntity.State.RUNNING);
        instance.setUpdatedAt(Instant.now());
        instanceRepo.save(instance);
    }

    @Override
    public void terminateProcessInstance(String processInstanceId, String reason) {
        ProcessInstanceEntity instance = instanceRepo.findById(processInstanceId).orElseThrow();
        instance.setState(ProcessInstanceEntity.State.TERMINATED);
        instance.setEndTime(Instant.now());
        instance.setDeleteReason(reason);
        instance.setUpdatedAt(Instant.now());
        instanceRepo.save(instance);
        historyService.recordProcessEnded(instance);

        // Mark all active executions as ended
        executionRepo.findByProcessInstanceId(processInstanceId).forEach(exec -> {
            exec.setIsActive(Boolean.FALSE);
            exec.setState(ExecutionEntity.State.ENDED);
            exec.setUpdatedAt(Instant.now());
            executionRepo.save(exec);
        });
    }

    @Override
    public ProcessInstanceResult startProcessByKeyAndVersion(String processKey, int version,
                                                             String businessKey,
                                                             Map<String, Object> variables) {
        ProcessDefinitionEntity def = definitionRepo.findByKeyAndVersion(processKey, version)
                .orElseThrow(() -> new EngineException(
                        "Process definition not found: " + processKey + " v" + version));
        guardNotSuspended(def);
        return startFromDefinition(def, businessKey, variables);
    }

    @Override
    public ProcessInstanceResult startProcessById(String processDefinitionId,
                                                   String businessKey,
                                                   Map<String, Object> variables) {
        ProcessDefinitionEntity def = definitionRepo.findById(processDefinitionId)
                .orElseThrow(() -> new EngineException(
                        "Process definition not found: " + processDefinitionId));
        guardNotSuspended(def);
        return startFromDefinition(def, businessKey, variables);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────────────────────────────────

    /** Shared start-from-definition logic used by all startProcess* overloads. */
    private ProcessInstanceResult startFromDefinition(ProcessDefinitionEntity def,
                                                       String businessKey,
                                                       Map<String, Object> variables) {
        BpmnModel model = getOrParseModel(def);
        Process   process = model.getProcesses().get(0);

        ProcessInstanceEntity instance = new ProcessInstanceEntity();
        instance.setId(UUID.randomUUID().toString());
        instance.setBusinessKey(businessKey);
        instance.setProcessDefinitionId(def.getId());
        instance.setProcessDefinitionKey(def.getKey());
        instance.setState(ProcessInstanceEntity.State.RUNNING);
        instance.setStartTime(Instant.now());
        instance.setCreatedAt(Instant.now());
        instance.setUpdatedAt(Instant.now());
        instanceRepo.save(instance);

        historyService.recordProcessStarted(instance, def);

        ExecutionEntity rootExec = new ExecutionEntity();
        rootExec.setId(UUID.randomUUID().toString());
        rootExec.setProcessInstanceId(instance.getId());
        rootExec.setIsScope(Boolean.TRUE);
        rootExec.setIsConcurrent(Boolean.FALSE);
        rootExec.setIsActive(Boolean.TRUE);
        rootExec.setState(ExecutionEntity.State.ACTIVE);
        rootExec.setCreatedAt(Instant.now());
        rootExec.setUpdatedAt(Instant.now());
        executionRepo.save(rootExec);

        if (variables != null) {
            variables.forEach((k, v) -> persistVariable(instance.getId(), rootExec.getId(), k, v));
        }

        ExecutionContext ctx = buildExecutionContext(rootExec, instance, model, process, variables);

        StartEvent startEvent = findNoneStartEvent(process);
        if (startEvent == null) {
            throw new EngineException("No none-startEvent found in process: " + def.getKey());
        }

        ctx.setCurrentFlowElement(startEvent);
        rootExec.setCurrentFlowNodeId(startEvent.getId());
        executionRepo.save(rootExec);

        executeElement(ctx);
        return toResult(instance);
    }

    private void guardNotSuspended(ProcessDefinitionEntity def) {
        if (def.isSuspended()) {
            throw new EngineException(
                    "Process definition '" + def.getKey() + "' v" + def.getVersion() +
                    " is suspended and cannot be started");
        }
    }

    private void ensureKeyExists(String processKey) {
        if (definitionRepo.findAllVersionsByKey(processKey).isEmpty()) {
            throw new EngineException("No process definition found for key: " + processKey);
        }
    }

    private ProcessDefinitionDto toDefinitionDto(ProcessDefinitionEntity def, boolean includeBpmn) {
        ProcessDefinitionDto dto = new ProcessDefinitionDto();
        dto.setId(def.getId());
        dto.setKey(def.getKey());
        dto.setVersion(def.getVersion());
        dto.setName(def.getName());
        dto.setSuspended(def.isSuspended());
        dto.setActive(def.isActive());
        dto.setResourceName(def.getResourceName());
        dto.setDeployedBy(def.getDeployedBy());
        dto.setDeployMessage(def.getDeployMessage());
        dto.setCreatedAt(def.getCreatedAt());
        dto.setUpdatedAt(def.getUpdatedAt());
        if (includeBpmn) {
            dto.setBpmnXml(def.getBpmnXml());
        }
        return dto;
    }

    private void executeFromAfter(ExecutionContext ctx, FlowElement completed) {
        // Close the open history record for the element that just finished its wait state
        historyService.recordActivityEnded(ctx, completed);

        // Take outgoing sequence flows
        List<SequenceFlow> outgoing = ctx.getProcess().getOutgoingFlows(completed.getId());
        if (outgoing.isEmpty()) {
            handleProcessEnd(ctx);
            return;
        }
        SequenceFlow nextFlow = outgoing.get(0);
        takeSequenceFlow(ctx, nextFlow);
        for (int i = 1; i < outgoing.size(); i++) {
            ExecutionContext branch = forkExecution(ctx);
            takeSequenceFlow(branch, outgoing.get(i));
            executeElement(branch);   // run each fork branch immediately
        }
        executeElement(ctx);
    }

    private ExecutionContext forkExecution(ExecutionContext parent) {
        ExecutionEntity newExec = new ExecutionEntity();
        newExec.setId(UUID.randomUUID().toString());
        newExec.setProcessInstanceId(parent.getProcessInstanceId());
        newExec.setParentExecutionId(parent.getId());
        newExec.setIsConcurrent(Boolean.TRUE);
        newExec.setIsActive(Boolean.TRUE);
        newExec.setState(ExecutionEntity.State.ACTIVE);
        newExec.setCreatedAt(Instant.now());
        newExec.setUpdatedAt(Instant.now());
        executionRepo.save(newExec);

        ExecutionContext branch = buildExecutionContextFromParent(parent, newExec);
        return branch;
    }

    private ExecutionContext buildExecutionContext(ExecutionEntity exec,
                                                   ProcessInstanceEntity instance,
                                                   BpmnModel model,
                                                   Process process,
                                                   Map<String, Object> variables) {
        ExecutionContext ctx = new ExecutionContext(exec.getId(),
                instance.getId(),
                instance.getProcessDefinitionId(),
                instance.getProcessDefinitionKey());
        ctx.setBpmnModel(model);
        ctx.setProcess(process);
        ctx.setScope(Boolean.TRUE.equals(exec.getIsScope()));
        ctx.setConcurrent(Boolean.TRUE.equals(exec.getIsConcurrent()));
        ctx.setActive(Boolean.TRUE.equals(exec.getIsActive()));
        if (variables != null) ctx.getVariables().putAll(variables);
        return ctx;
    }

    private ExecutionContext buildExecutionContextFromParent(ExecutionContext parent, ExecutionEntity exec) {
        ExecutionContext ctx = new ExecutionContext(exec.getId(),
                parent.getProcessInstanceId(),
                parent.getProcessDefinitionId(),
                parent.getProcessDefinitionKey());
        ctx.setBpmnModel(parent.getBpmnModel());
        ctx.setProcess(parent.getProcess());
        ctx.setParent(parent);
        ctx.setConcurrent(true);
        return ctx;
    }

    private StartEvent findNoneStartEvent(Process process) {
        return process.getFlowElements().stream()
                .filter(fe -> fe instanceof StartEvent se && se.getEventDefinitions().isEmpty())
                .map(fe -> (StartEvent) fe)
                .findFirst().orElse(null);
    }

    private BpmnModel getOrParseModel(ProcessDefinitionEntity def) {
        return modelCache.computeIfAbsent(def.getId(), k -> parser.parse(def.getBpmnXml()));
    }

    private void persistExecutionState(ExecutionContext ctx) {
        executionRepo.findById(ctx.getId()).ifPresent(exec -> {
            exec.setCurrentFlowNodeId(ctx.getCurrentFlowElementId());
            exec.setIsActive(ctx.isActive());
            exec.setIsScope(ctx.isScope());
            exec.setIsConcurrent(ctx.isConcurrent());
            exec.setState(ctx.isEnded() ? ExecutionEntity.State.ENDED : ExecutionEntity.State.ACTIVE);
            exec.setUpdatedAt(Instant.now());
            executionRepo.save(exec);
        });
    }

    private void persistVariable(String instanceId, String execId, String name, Object value) {
        ProcessVariableEntity var = variableRepo.findByProcessInstanceIdAndName(instanceId, name)
                .orElse(new ProcessVariableEntity());
        var.setId(var.getId() != null ? var.getId() : UUID.randomUUID().toString());
        var.setProcessInstanceId(instanceId);
        var.setExecutionId(execId);
        var.setName(name);
        VariableMapper.mapToEntity(var, value);
        var.setUpdatedAt(Instant.now());
        if (var.getCreatedAt() == null) var.setCreatedAt(Instant.now());
        variableRepo.save(var);
    }

    private Map<String, Object> loadVariables(String instanceId) {
        Map<String, Object> vars = new HashMap<>();
        variableRepo.findByProcessInstanceId(instanceId)
                .forEach(v -> vars.put(v.getName(), VariableMapper.mapFromEntity(v)));
        return vars;
    }

    private ProcessInstanceResult toResult(ProcessInstanceEntity instance) {
        return ProcessInstanceResult.builder()
                .id(instance.getId())
                .businessKey(instance.getBusinessKey())
                .processDefinitionId(instance.getProcessDefinitionId())
                .processDefinitionKey(instance.getProcessDefinitionKey())
                .state(instance.getState().name())
                .startTime(instance.getStartTime())
                .endTime(instance.getEndTime())
                .build();
    }
}
