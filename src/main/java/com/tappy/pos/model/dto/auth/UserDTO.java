package com.tappy.pos.model.dto.auth;

import lombok.*;

/**
 * UserDTO DTO - User information for API responses
 */
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
}