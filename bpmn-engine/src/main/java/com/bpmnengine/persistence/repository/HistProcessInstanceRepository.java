package com.bpmnengine.persistence.repository;

import com.bpmnengine.persistence.entity.HistProcessInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistProcessInstanceRepository extends JpaRepository<HistProcessInstanceEntity, String> {
    List<HistProcessInstanceEntity> findByProcessDefinitionKey(String key);
}
