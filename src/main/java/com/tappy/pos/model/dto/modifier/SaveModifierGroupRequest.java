package com.tappy.pos.model.dto.modifier;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
public class SaveModifierGroupRequest {

    @NotBlank
    private String name;

    private Integer minSelect;
    private Integer maxSelect;
    private Boolean required;
    private Integer sortOrder;

    @Valid
    private List<OptionRequest> options;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OptionRequest {
        @NotBlank
        private String name;
        private BigDecimal priceDelta;
        private Integer sortOrder;
    }
}
