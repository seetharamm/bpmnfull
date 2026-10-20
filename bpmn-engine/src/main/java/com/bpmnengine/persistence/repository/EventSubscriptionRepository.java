package com.bpmnengine.persistence.repository;

import com.bpmnengine.persistence.entity.EventSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventSubscriptionRepository extends JpaRepository<EventSubscriptionEntity, String> {
    List<EventSubscriptionEntity> findByEventTypeAndEventName(String eventType, String eventName);
    List<EventSubscriptionEntity> findByExecutionId(String executionId);
    List<EventSubscriptionEntity> findByExecutionIdAndEventType(String executionId, String eventType);
    List<EventSubscriptionEntity> findByProcessInstanceId(String processInstanceId);
}
