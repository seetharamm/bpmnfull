package com.bpmnengine.persistence.repository;

import com.bpmnengine.persistence.entity.HistActivityInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface HistActivityInstanceRepository extends JpaRepository<HistActivityInstanceEntity, String> {
    List<HistActivityInstanceEntity> findByProcessInstanceId(String processInstanceId);
    List<HistActivityInstanceEntity> findByExecutionId(String executionId);

    /** All open (endTime IS NULL) history rows for a given activity across the whole instance. */
    @Query("SELECT h FROM HistActivityInstanceEntity h WHERE h.processInstanceId = :piId AND h.activityId = :actId AND h.endTime IS NULL")
    List<HistActivityInstanceEntity> findOpenByProcessInstanceIdAndActivityId(
            @Param("piId") String processInstanceId,
            @Param("actId") String activityId);
}
