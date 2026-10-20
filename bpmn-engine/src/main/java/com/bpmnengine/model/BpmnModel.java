package com.bpmnengine.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Root model class representing a parsed BPMN 2.0 &lt;definitions&gt; element.
 */
@Data
public class BpmnModel {

    private String id;
    private String name;
    private String targetNamespace;
    private String exporter;
    private String exporterVersion;

    /** All processes (including subprocesses at top level) declared in this model. */
    private List<Process> processes = new ArrayList<>();

    /** Global messages declared in the &lt;definitions&gt; scope. */
    private List<Message> messages = new ArrayList<>();

    /** Global signals. */
    private List<Signal> signals = new ArrayList<>();

    /** Global errors. */
    private List<BpmnError> errors = new ArrayList<>();

    /** Global escalations. */
    private List<Escalation> escalations = new ArrayList<>();

    /** Global item definitions. */
    private List<ItemDefinition> itemDefinitions = new ArrayList<>();

    public Process getProcessById(String id) {
        return processes.stream()
                .filter(p -> id.equals(p.getId()))
                .findFirst().orElse(null);
    }

    public Message getMessageByName(String messageName) {
        return messages.stream()
                .filter(m -> messageName.equals(m.getName()))
                .findFirst().orElse(null);
    }

    public Signal getSignalByName(String signalName) {
        return signals.stream()
                .filter(s -> signalName.equals(s.getName()))
                .findFirst().orElse(null);
    }
}
