package com.barbershop.model.dto.customer;

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

