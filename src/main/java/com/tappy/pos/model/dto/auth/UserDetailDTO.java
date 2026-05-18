package com.tappy.pos.model.dto.auth;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetailDTO {
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
    private Long vendorId;
    private String vendorName;
    private Set<RoleDTO> roles;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

