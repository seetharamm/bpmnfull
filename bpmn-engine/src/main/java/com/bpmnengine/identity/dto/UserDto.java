package com.bpmnengine.identity.dto;

import lombok.Data;
import java.time.Instant;
import java.util.Set;

/** Read-only user view — password is never included. */
@Data
public class UserDto {
    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String displayName;
    private boolean enabled;
    private boolean locked;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastLoginAt;
    private Set<String> roles;       // direct role names
    private Set<String> groups;      // group names the user belongs to
    private Set<String> effectiveRoles; // direct + inherited
}
