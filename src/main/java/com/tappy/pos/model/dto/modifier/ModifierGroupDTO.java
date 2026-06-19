package com.tappy.pos.model.dto.modifier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifierGroupDTO {

    private Long id;
    private String name;
    private Integer minSelect;
    private Integer maxSelect;
    private Boolean required;
    private Integer sortOrder;
    private List<OptionDTO> options;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OptionDTO {
        private Long id;
        private String name;
        private BigDecimal priceDelta;
        private Integer sortOrder;
    }
}
