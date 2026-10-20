package com.bpmnengine.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity @Table(name = "process_definition")
@Data
public class ProcessDefinitionEntity {

    @Id private String id;
    @Column(name = "process_key", nullable = false) private String key;
    @Column(nullable = false) private int version = 1;
    @Column(name = "proc_name") private String name;
    private String description;
    @Column(name = "bpmn_xml", nullable = false, columnDefinition = "TEXT") private String bpmnXml;
    @Column(name = "resource_name") private String resourceName;
    @Column(name = "deployment_id") private String deploymentId;
    private boolean suspended = false;
    /** True when this is the designated active version for its key. */
    private boolean active = true;
    @Column(name = "deployed_by") private String deployedBy;
    @Column(name = "deploy_message", columnDefinition = "TEXT") private String deployMessage;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
}
