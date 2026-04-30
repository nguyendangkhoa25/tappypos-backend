package com.knp.model.entity.customer;

import jakarta.persistence.*;
import com.knp.model.entity.BaseEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    // Identity Card Information
    @Column(name = "id_card_number", unique = true)
    private String idCardNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender")
    private String gender;

    @Column(name = "id_card_issued_date")
    private LocalDate idCardIssuedDate;

    @Column(name = "id_card_issued_place")
    private String idCardIssuedPlace;

    @Column(name = "permanent_address", length = 500)
    private String permanentAddress;

    // Loyalty
    @Builder.Default
    @Column(name = "loyalty_points", nullable = false)
    private Integer loyaltyPoints = 0;

    @Builder.Default
    @Column(name = "total_spent", nullable = false)
    private BigDecimal totalSpent = BigDecimal.ZERO;
}

