package com.barbershop.model.dto;

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
    private LocalDateTime createdAt;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCustomerRequest {
    private String name;
    private String phone;
    private String email;
    private String notes;
}

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
}

