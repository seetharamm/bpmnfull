package com.bpmnengine.identity.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * A system user that can be assigned to tasks, own processes, and belong to groups.
 * Password is stored as a BCrypt hash — never in plain text.
 */
@Entity
@Table(name = "engine_user")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class UserEntity {

    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id;

    @Column(nullable = false, unique = true)
    @ToString.Include
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt hash of the user's password. Never exposed via API. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "display_name")
    private String displayName;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private boolean locked = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /** Roles directly granted to this user. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_role",
        joinColumns        = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleEntity> roles = new HashSet<>();

    /** Group memberships for this user. */
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private Set<MembershipEntity> memberships = new HashSet<>();
}
