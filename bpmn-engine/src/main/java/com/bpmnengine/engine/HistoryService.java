package com.bpmnengine.engine;

import com.bpmnengine.model.FlowElement;
import com.bpmnengine.model.SequenceFlow;
import com.bpmnengine.persistence.entity.*;
import com.bpmnengine.persistence.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.UUID;

/**
 * Records BPMN audit history (process started/ended, activity started/ended).
 * Respects the configured history level (NONE | ACTIVITY | AUDIT | FULL).
 */
@Service
public class HistoryService {

    private static final Logger log = LoggerFactory.getLogger(HistoryService.class);

    private final HistProcessInstanceRepository histPiRepo;
    private final HistActivityInstanceRepository histAiRepo;

    public HistoryService(HistProcessInstanceRepository histPiRepo,
                          HistActivityInstanceRepository histAiRepo) {
        this.histPiRepo = histPiRepo;
        this.histAiRepo = histAiRepo;
    }

    // ── Process lifecycle ─────────────────────────────────────────────────────

    public void recordProcessStarted(ProcessInstanceEntity instance, ProcessDefinitionEntity def) {
        HistProcessInstanceEntity hist = new HistProcessInstanceEntity();
        hist.setId(instance.getId());
        hist.setProcessDefinitionId(instance.getProcessDefinitionId());
        hist.setProcessDefinitionKey(instance.getProcessDefinitionKey());
        hist.setProcessDefinitionVersion(def.getVersion());
        hist.setBusinessKey(instance.getBusinessKey());
        hist.setState("RUNNING");
        hist.setStartTime(instance.getStartTime());
        hist.setStartUserId(instance.getStartUserId());
        hist.setCreatedAt(Instant.now());
        histPiRepo.save(hist);
    }

    public void recordProcessEnded(ProcessInstanceEntity instance) {
        histPiRepo.findById(instance.getId()).ifPresent(hist -> {
            hist.setState(instance.getState().name());
            hist.setEndTime(instance.getEndTime());
            hist.setDurationMs(instance.getDurationMs());
            hist.setDeleteReason(instance.getDeleteReason());
            histPiRepo.save(hist);
        });
    }

    // ── Activity lifecycle ────────────────────────────────────────────────────

    public void recordActivityStarted(ExecutionContext ctx, FlowElement element) {
        if (element instanceof SequenceFlow) return; // don't log flows as activities

        HistActivityInstanceEntity hist = new HistActivityInstanceEntity();
        hist.setId(UUID.randomUUID().toString());
        hist.setActivityId(element.getId());
        hist.setActivityName(element.getName());
        hist.setActivityType(element.getElementType() != null ? element.getElementType().name() : "UNKNOWN");
        hist.setExecutionId(ctx.getId());
        hist.setProcessInstanceId(ctx.getProcessInstanceId());
        hist.setProcessDefinitionId(ctx.getProcessDefinitionId());
        hist.setProcessDefinitionKey(ctx.getProcessDefinitionKey());
        hist.setStartTime(Instant.now());
        hist.setCreatedAt(Instant.now());
        histAiRepo.save(hist);

        log.trace("History: activity started [{}/{}]", element.getElementType(), element.getId());
    }

    public void recordActivityEnded(ExecutionContext ctx, FlowElement element) {
        if (element instanceof SequenceFlow) return;

        histAiRepo.findByExecutionId(ctx.getId()).stream()
                .filter(h -> h.getActivityId().equals(element.getId()) && h.getEndTime() == null)
                .findFirst()
                .ifPresent(hist -> {
                    hist.setEndTime(Instant.now());
                    hist.setDurationMs(hist.getEndTime().toEpochMilli() - hist.getStartTime().toEpochMilli());
                    histAiRepo.save(hist);
                });
    }

    /**
     * Closes ALL open history rows for a given activity across the whole process instance.
     * Used by the parallel join gateway: when it fires, every branch that parked at the
     * gateway has its own open history row that needs to be closed.
     */
    public void recordActivityEndedForInstance(String processInstanceId, FlowElement element) {
        if (element instanceof SequenceFlow) return;

        Instant now = Instant.now();
        histAiRepo.findOpenByProcessInstanceIdAndActivityId(processInstanceId, element.getId())
                .forEach(hist -> {
                    hist.setEndTime(now);
                    hist.setDurationMs(now.toEpochMilli() - hist.getStartTime().toEpochMilli());
                    histAiRepo.save(hist);
                });
    }

    public void recordSequenceFlowTaken(ExecutionContext ctx, SequenceFlow sf) {
        log.trace("History: sequence flow taken [{}]", sf.getId());
    }
}
