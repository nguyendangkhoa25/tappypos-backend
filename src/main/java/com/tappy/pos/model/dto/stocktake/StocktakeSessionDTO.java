package com.tappy.pos.model.dto.stocktake;

import com.tappy.pos.model.enums.StocktakeStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A stocktake session. {@code counts} is populated only on the detail endpoint.
 */
@Data
@Builder
public class StocktakeSessionDTO {
    private Long id;
    private String name;
    private StocktakeStatus status;
    private String note;
    private String startedBy;
    private LocalDateTime startedAt;
    private String completedBy;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    /** Number of products counted so far in this session. */
    private long countedItems;
    /** Number of counted products whose physical count differs from the system. */
    private long discrepancyCount;

    /** Counted lines — only set on the session-detail response. */
    private List<StocktakeCountDTO> counts;
}
