package com.tappy.pos.model.dto.repair;

import lombok.Data;

@Data
public class AssignTechnicianRequest {
    private Long technicianId;
    private String technicianName;
}
