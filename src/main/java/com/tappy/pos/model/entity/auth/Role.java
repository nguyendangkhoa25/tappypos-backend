package com.tappy.pos.model.entity.auth;

import jakarta.persistence.*;
import com.tappy.pos.model.entity.UnifiedTenantEntity;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends UnifiedTenantEntity {

    @Column(unique = true, nullable = false, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    @Builder.Default
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<User> users = new HashSet<>();

    // Constructor for convenience
    public Role(String name, String description) {
        this.name = name;
        this.description = description;
    }
}

