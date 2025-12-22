package com.barbershop.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends BaseEntity {

    @NotBlank(message = "Customer name is required")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Phone number is required")
    @Column(nullable = false, unique = true)
    private String phone;

    @Email(message = "Email should be valid")
    private String email;

    @Column(length = 500)
    private String notes;

    // Social IDs
    @Column(name = "zalo_id")
    private String zaloId;

    @Column(name = "facebook_id")
    private String facebookId;

    // Customer Habits
    @Column(name = "preferred_services", length = 500)
    private String preferredServices;

    @Column(name = "allergies_or_sensitivities", length = 500)
    private String allergiesOrSensitivities;

    @Column(name = "hair_type")
    private String hairType;

    @Column(name = "special_requests", length = 500)
    private String specialRequests;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> orders = new ArrayList<>();
}

