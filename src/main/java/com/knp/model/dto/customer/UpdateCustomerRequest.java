package com.knp.model.dto.customer;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCustomerRequest {
    private String name;
    private String phone;
    private String email;
    private String notes;

    // Social IDs
    private String zaloId;
    private String facebookId;

    // Customer Habits
    private String preferredServices;
    private String allergiesOrSensitivities;
    private String hairType;
    private String specialRequests;

    // Identity Card Information
    private String idCardNumber;
    private LocalDate dateOfBirth;
    private String gender;
    private LocalDate idCardIssuedDate;
    private String idCardIssuedPlace;
    private String permanentAddress;
}
