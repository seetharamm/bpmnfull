package com.bpmnengine.identity.dto;

import lombok.Data;
import java.time.Instant;
import java.util.Set;

@Data
public class RoleDto {
    private String id;
    private String name;
    private String description;
    private Instant createdAt;
    private int userCount;   // how many users have this role (direct + inherited)
}
