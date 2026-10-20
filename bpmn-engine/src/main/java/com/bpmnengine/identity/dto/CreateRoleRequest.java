package com.bpmnengine.identity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateRoleRequest {
    @NotBlank private String name;
    private String description;
}
