package com.bpmnengine.persistence.repository;

import com.bpmnengine.persistence.entity.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface JobRepository extends JpaRepository<JobEntity, String> {
    List<JobEntity> findByProcessInstanceId(String processInstanceId);
    List<JobEntity> findByDuedateBefore(Instant now);
    List<JobEntity> findByType(String type);
}
