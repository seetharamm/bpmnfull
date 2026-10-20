package com.bpmnengine.persistence.repository;

import com.bpmnengine.persistence.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TaskEntityRepository extends JpaRepository<TaskEntity, String> {

    @Query("SELECT t FROM TaskEntity t WHERE t.processInstanceId = :instanceId ORDER BY t.createdAt ASC")
    List<TaskEntity> findByProcessInstanceId(@Param("instanceId") String processInstanceId);

    List<TaskEntity> findByAssignee(String assignee);
    List<TaskEntity> findByState(TaskEntity.State state);
    List<TaskEntity> findByStateIn(List<TaskEntity.State> states);
    Optional<TaskEntity> findByExecutionIdAndTaskDefinitionKey(String executionId, String taskDefinitionKey);
}
