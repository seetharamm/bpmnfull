package com.bpmnengine.engine;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.Map;

/** DTO returned from engine operations that involve process instances. */
@Data
@Builder
public class ProcessInstanceResult {
    private String id;
    private String businessKey;
    private String processDefinitionId;
    private String processDefinitionKey;
    private int    processDefinitionVersion;
    private String state;
    private Instant startTime;
    private Instant endTime;
    private Map<String, Object> variables;
}
