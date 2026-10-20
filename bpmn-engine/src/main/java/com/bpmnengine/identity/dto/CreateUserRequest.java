package com.bpmnengine.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Set;

/** Request body for creating a new user. */
@Data
public class CreateUserRequest {
    @NotBlank  private String username;
    @NotBlank @Email private String email;
    @NotBlank @Size(min = 6, message = "Password must be at least 6 characters")
               private String password;
    private String firstName;
    private String lastName;
    private String displayName;
    private Set<String> roleNames;   // optional — role names to assign immediately
    private Set<String> groupNames;  // optional — group names to join immediately
}
