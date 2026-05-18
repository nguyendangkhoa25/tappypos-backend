package com.tappy.pos.model.dto.employee;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaySalaryRequest {
    private boolean sendNotification = true;
}
