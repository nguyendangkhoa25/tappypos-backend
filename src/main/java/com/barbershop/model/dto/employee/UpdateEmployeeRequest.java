package com.barbershop.model.dto.employee;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEmployeeRequest {
    private String name;
    private String phone;
    private String email;
    private String position;
    private String status;
}
