package com.bpmnengine.identity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.Set;

@Data
public class CreateGroupRequest {
    @NotBlank private String name;
    private String description;
    private String type = "ORGANISATIONAL";
    private String parentId;
    private Set<String> roleNames;  // optional – assign roles immediately
}
