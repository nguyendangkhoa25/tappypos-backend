package com.tappy.pos.model.dto.bank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankDTO {
    private Long id;
    private String code;
    private String bin;
    private String name;
    private String shortName;
    private Integer sortOrder;
    private Boolean isActive;
}
