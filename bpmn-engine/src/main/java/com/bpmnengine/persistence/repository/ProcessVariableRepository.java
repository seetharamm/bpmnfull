package com.bpmnengine.persistence.repository;

import com.bpmnengine.persistence.entity.ProcessVariableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProcessVariableRepository extends JpaRepository<ProcessVariableEntity, String> {
    List<ProcessVariableEntity> findByProcessInstanceId(String processInstanceId);
    Optional<ProcessVariableEntity> findByProcessInstanceIdAndName(String processInstanceId, String name);
    List<ProcessVariableEntity> findByExecutionId(String executionId);
}
