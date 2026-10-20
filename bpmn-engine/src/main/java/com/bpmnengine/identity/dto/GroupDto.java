package com.bpmnengine.identity.dto;

import lombok.Data;
import java.time.Instant;
import java.util.Set;

@Data
public class GroupDto {
    private String id;
    private String name;
    private String description;
    private String type;
    private String parentId;
    private Instant createdAt;
    private Set<String> roles;        // role names assigned to this group
    private int memberCount;
}
