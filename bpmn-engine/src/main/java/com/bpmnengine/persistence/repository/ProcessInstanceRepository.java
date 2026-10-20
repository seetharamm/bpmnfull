package com.bpmnengine.persistence.repository;

import com.bpmnengine.persistence.entity.ProcessInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProcessInstanceRepository extends JpaRepository<ProcessInstanceEntity, String> {
    List<ProcessInstanceEntity> findByProcessDefinitionKey(String key);
    List<ProcessInstanceEntity> findByBusinessKey(String businessKey);
    List<ProcessInstanceEntity> findByState(ProcessInstanceEntity.State state);

    /** Count running instances for a specific definition version. */
    long countByProcessDefinitionIdAndState(String processDefinitionId,
                                            ProcessInstanceEntity.State state);

    /** Count running instances for a process key (any version). */
    @Query("SELECT COUNT(i) FROM ProcessInstanceEntity i " +
           "WHERE i.processDefinitionKey = :key AND i.state = 'RUNNING'")
    long countRunningByKey(@Param("key") String key);
}
