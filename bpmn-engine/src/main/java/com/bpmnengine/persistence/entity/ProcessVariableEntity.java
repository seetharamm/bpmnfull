package com.bpmnengine.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity @Table(name = "process_variable")
@Data
public class ProcessVariableEntity {

    @Id private String id;
    @Column(name = "process_instance_id", nullable = false) private String processInstanceId;
    @Column(name = "execution_id")   private String executionId;
    @Column(name = "var_name", nullable = false) private String name;
    @Column(name = "var_type", nullable = false) private String type;
    @Column(name = "text_value",   columnDefinition = "TEXT") private String textValue;
    @Column(name = "long_value")   private Long longValue;
    @Column(name = "double_value") private Double doubleValue;
    @Column(name = "bytes_value")  private byte[] bytesValue;
    @Column(name = "is_concurrent_local") private boolean concurrentLocal = false;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
}
