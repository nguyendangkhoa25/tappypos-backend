package com.tappy.pos.model.dto.employee;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GenerateSalaryRequest {
    private Integer month;
    private Integer year;
}
