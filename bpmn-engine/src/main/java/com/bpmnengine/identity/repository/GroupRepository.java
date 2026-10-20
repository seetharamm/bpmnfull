package com.bpmnengine.identity.repository;

import com.bpmnengine.identity.entity.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<GroupEntity, String> {
    Optional<GroupEntity> findByName(String name);
    boolean existsByName(String name);
    List<GroupEntity> findByType(String type);

    @Query("SELECT g FROM GroupEntity g JOIN g.memberships m WHERE m.user.id = :userId")
    List<GroupEntity> findGroupsByUserId(@Param("userId") String userId);

    @Query("SELECT g FROM GroupEntity g JOIN g.memberships m WHERE m.user.username = :username")
    List<GroupEntity> findGroupsByUsername(@Param("username") String username);
}
