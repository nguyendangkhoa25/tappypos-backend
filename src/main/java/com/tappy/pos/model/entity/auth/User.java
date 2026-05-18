package com.tappy.pos.model.entity.auth;

import jakarta.persistence.*;
import com.tappy.pos.model.entity.UnifiedTenantEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends UnifiedTenantEntity {

    @NotBlank(message = "Username is required")
    @Column(nullable = false, length = 50)
    private String username;

    @Email(message = "Email should be valid")
    @Column(length = 100)
    private String email;

    @NotBlank(message = "Password is required")
    @Column(nullable = false)
    private String password;

    @Column(length = 100)
    private String fullName;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean accountNonLocked = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean credentialsNonExpired = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean accountNonExpired = true;

    @Column(name = "require_action", nullable = false)
    private String requireAction;

    @Column(columnDefinition = "LONGTEXT")
    private String avatar;

    @Column(length = 50)
    private String colorPreference;

    @Builder.Default
    @Column(length = 10)
    private String lang = "vi";

    // ...existing code...
    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Builder.Default
    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    @Column(length = 255)
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String preferences;

    @Column(name = "pin_hash", length = 255)
    private String pinHash;

    @Column(name = "nickname", length = 100)
    private String nickname;

    // Add a role to this user
    public void addRole(Role role) {
        this.roles.add(role);
        role.getUsers().add(this);
    }

    // Remove a role from this user
    public void removeRole(Role role) {
        this.roles.remove(role);
        role.getUsers().remove(this);
    }
}

