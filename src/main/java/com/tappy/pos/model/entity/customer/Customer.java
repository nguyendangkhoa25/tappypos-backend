package com.tappy.pos.model.entity.customer;

import jakarta.persistence.*;
import com.tappy.pos.model.entity.TenantAwareEntity;
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
public class Customer extends TenantAwareEntity {

    @NotBlank(message = "Customer name is required")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Phone number is required")
    @Column(nullable = false, unique = true)
    private String phone;

    @Email(message = "Email should be valid")
    private String email;

    /** RETAIL (default) | WHOLESALE — wholesale customers (e.g. contractors) get product wholesale prices. */
    @Builder.Default
    @Column(name = "customer_type", length = 20, nullable = false)
    private String customerType = "RETAIL";

    @Column(length = 500)
    private String notes;

    /** CCCD/CMND for KYC on large gold buys / pawn (jewelry). Optional; blank for most shop types. */
    @Column(name = "id_number", length = 50)
    private String idNumber;

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

    @Builder.Default
    @Column(name = "walk_in", nullable = false)
    private boolean walkIn = false;

    // Loyalty
    @Builder.Default
    @Column(name = "loyalty_points", nullable = false)
    private Integer loyaltyPoints = 0;

    @Builder.Default
    @Column(name = "total_spent", nullable = false)
    private BigDecimal totalSpent = BigDecimal.ZERO;

    /** Stamps on the customer's current (unfilled) stamp card — "mua N ly tặng 1". */
    @Builder.Default
    @Column(name = "stamp_count", nullable = false)
    private Integer stampCount = 0;

    /** Filled stamp cards converted to free-item rewards, not yet redeemed. */
    @Builder.Default
    @Column(name = "stamp_rewards", nullable = false)
    private Integer stampRewards = 0;

    /** R2 public URL for the customer's avatar photo. */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
}

