package com.knp.model.dto;

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
    private Set<String> roleNames;
    private String notes;
}

