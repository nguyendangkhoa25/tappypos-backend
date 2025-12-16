package com.barbershop.model.dto.customer;

import lombok.*;

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
