package com.knp.model.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private Boolean active;
    private Boolean accountNonLocked;
    private Integer failedLoginAttempts;
    private Boolean credentialsNonExpired;
    private Boolean accountNonExpired;
    private String requireAction;
    private Long employeeId;
    private Set<RoleDTO> roles;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

