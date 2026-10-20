package com.bpmnengine.api.dto;

import lombok.Data;
import java.util.List;

/** Summary of all versions for one process key. */
@Data
public class ProcessVersionSummaryDto {
    private String key;
    private String name;
    private int    latestVersion;
    private int    activeVersion;
    private boolean suspended;
    private long   totalVersions;
    private long   totalRunningInstances;
    private List<ProcessDefinitionDto> versions;
}
