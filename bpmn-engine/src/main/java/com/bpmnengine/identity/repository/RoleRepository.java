package com.bpmnengine.identity.repository;

import com.bpmnengine.identity.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, String> {
    Optional<RoleEntity> findByName(String name);
    boolean existsByName(String name);
    List<RoleEntity> findByNameIn(List<String> names);
}
