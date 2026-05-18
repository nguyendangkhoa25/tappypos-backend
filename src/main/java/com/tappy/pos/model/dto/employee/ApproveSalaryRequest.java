package com.tappy.pos.model.dto.employee;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ApproveSalaryRequest {
    private boolean sendNotification = true;
}
