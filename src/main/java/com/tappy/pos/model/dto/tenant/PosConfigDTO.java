package com.tappy.pos.model.dto.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosConfigDTO {
    private String posMode;
    private Boolean autoPrint;
    private Boolean vatEnabled;
    private String cashDenominations;
    private List<String> quickPhrases;
}
