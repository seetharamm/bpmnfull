package com.bpmnengine.identity.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Organisational or security group (e.g. hr_team, it_team).
 * Users join groups via {@link MembershipEntity}.
 * Groups carry roles which are inherited by all their members.
 */
@Entity
@Table(name = "identity_group")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class GroupEntity {

    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id;

    @Column(nullable = false, unique = true)
    @ToString.Include
    private String name;

    private String description;

    @Column(nullable = false)
    private String type = "ORGANISATIONAL"; // ORGANISATIONAL | SECURITY | SYSTEM

    @Column(name = "parent_id")
    private String parentId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Members of this group. */
    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private Set<MembershipEntity> memberships = new HashSet<>();

    /** Roles assigned to this group (inherited by all members). */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "group_role",
        joinColumns        = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleEntity> roles = new HashSet<>();
}
