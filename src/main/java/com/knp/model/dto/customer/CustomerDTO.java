package com.knp.model.dto.customer;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerDTO {
    private Long id;
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

    private LocalDateTime createdAt;
}

