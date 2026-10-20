package com.bpmnengine.identity.repository;

import com.bpmnengine.identity.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<UserEntity> findByEnabled(boolean enabled);

    Page<UserEntity> findAll(Pageable pageable);

    /** Users that are a member of a given group (by group name). */
    @Query("SELECT u FROM UserEntity u JOIN u.memberships m WHERE m.group.name = :groupName")
    List<UserEntity> findByGroupName(@Param("groupName") String groupName);

    /** Users that have a specific role directly assigned. */
    @Query("SELECT u FROM UserEntity u JOIN u.roles r WHERE r.name = :roleName")
    List<UserEntity> findByDirectRoleName(@Param("roleName") String roleName);

    /** Users that have a role either directly or through group membership. */
    @Query("""
        SELECT DISTINCT u FROM UserEntity u
        WHERE EXISTS (
            SELECT 1 FROM u.roles r WHERE r.name = :roleName
        ) OR EXISTS (
            SELECT 1 FROM u.memberships m JOIN m.group.roles gr WHERE gr.name = :roleName
        )
        """)
    List<UserEntity> findByEffectiveRoleName(@Param("roleName") String roleName);
}
