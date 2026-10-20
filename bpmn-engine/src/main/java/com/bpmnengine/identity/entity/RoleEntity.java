package com.bpmnengine.identity.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * A named permission role (e.g. ADMIN, HR_MANAGER, IT_STAFF).
 * Roles can be assigned directly to users or inherited through group membership.
 */
@Entity
@Table(name = "role")
@Data
public class RoleEntity {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Users that have this role directly assigned. */
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<UserEntity> users = new HashSet<>();

    /** Groups that carry this role. */
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<GroupEntity> groups = new HashSet<>();
}
