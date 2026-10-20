package com.bpmnengine.identity.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;
import java.util.Set;

/** Request body for updating a user (all fields optional). */
@Data
public class UpdateUserRequest {
    @Email   private String email;
    private String firstName;
    private String lastName;
    private String displayName;
    private Boolean enabled;
    private Boolean locked;
    private String newPassword;    // if present, password will be rehashed
}
