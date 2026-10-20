package com.bpmnengine.identity.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.time.Instant;

/**
 * A user's membership in a group.
 * Joining a group grants the user all roles assigned to that group.
 */
@Entity
@Table(name = "membership",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "group_id"}))
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class MembershipEntity {

    @Id
    @EqualsAndHashCode.Include
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Include
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    @ToString.Include
    private GroupEntity group;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "added_by")
    private String addedBy;
}
