package com.tappy.pos.model.dto.auth;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {
    private String username;
    private String email;
    private String password;
    private String fullName;
    private Long employeeId;
    private Long vendorId;
    private Set<String> roleNames;
    private String notes;
    /**
     * Optional per-user feature overrides. When non-empty, the JWT features for this
     * user become: tenant_features ∩ featureNames (overrides role_features intersection).
     * When null or empty, the default role-based feature resolution applies.
     */
    private Set<String> featureNames;
}

