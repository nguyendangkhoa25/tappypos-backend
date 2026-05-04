package com.knp.model.dto.pawn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PawnGeneralDetail {
    private String serialNumber;
    private String condition;
}
