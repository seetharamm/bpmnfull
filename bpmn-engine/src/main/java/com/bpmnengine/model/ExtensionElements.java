package com.bpmnengine.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Holds extension elements — engine-specific attributes and properties. */
@Data
public class ExtensionElements {
    /** Key→value properties (e.g. activiti:property). */
    private Map<String, String> properties = new HashMap<>();
    /** Execution listeners. */
    private List<ExecutionListener> executionListeners = new ArrayList<>();
    /** Task listeners (for user tasks). */
    private List<TaskListener> taskListeners = new ArrayList<>();
}
