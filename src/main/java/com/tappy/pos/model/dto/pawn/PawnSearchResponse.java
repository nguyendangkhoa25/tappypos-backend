package com.tappy.pos.model.dto.pawn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PawnSearchResponse {
    private List<PawnResponse> content;
    private long totalElements;
    private int totalPages;
    private int size;
    private int number;
    private PawnSummary summary;

    public static PawnSearchResponse from(Page<PawnResponse> page, PawnSummary summary) {
        return PawnSearchResponse.builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .size(page.getSize())
                .number(page.getNumber())
                .summary(summary)
                .build();
    }
}
