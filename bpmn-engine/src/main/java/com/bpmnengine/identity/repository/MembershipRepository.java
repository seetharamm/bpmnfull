package com.bpmnengine.identity.repository;

import com.bpmnengine.identity.entity.MembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<MembershipEntity, String> {
    List<MembershipEntity> findByUserId(String userId);
    List<MembershipEntity> findByGroupId(String groupId);
    Optional<MembershipEntity> findByUserIdAndGroupId(String userId, String groupId);
    boolean existsByUserIdAndGroupId(String userId, String groupId);
    void deleteByUserIdAndGroupId(String userId, String groupId);
}
