package com.bpmnengine.persistence.repository;

import com.bpmnengine.persistence.entity.ExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ExecutionRepository extends JpaRepository<ExecutionEntity, String> {
    List<ExecutionEntity> findByProcessInstanceId(String processInstanceId);

    // Only active (non-ended) executions parked at a given node — used by parallel join
    @Query("SELECT e FROM ExecutionEntity e WHERE e.processInstanceId = :piId AND e.currentFlowNodeId = :nodeId AND e.isActive = true")
    List<ExecutionEntity> findByProcessInstanceIdAndCurrentFlowNodeId(@Param("piId") String processInstanceId,
                                                                       @Param("nodeId") String flowNodeId);

    @Query("SELECT COUNT(e) FROM ExecutionEntity e WHERE e.processInstanceId = :piId AND e.isActive = true")
    long countActiveByProcessInstanceId(@Param("piId") String processInstanceId);

    // Only count active executions at a node — prevents stale/ended rows from inflating the join count
    @Query("SELECT COUNT(e) FROM ExecutionEntity e WHERE e.processInstanceId = :piId AND e.currentFlowNodeId = :nodeId AND e.isActive = true")
    long countByProcessInstanceIdAndCurrentFlowNodeId(@Param("piId") String processInstanceId,
                                                       @Param("nodeId") String flowNodeId);
}
