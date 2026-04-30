package com.knp.model.dto.pawn;

import com.knp.model.enums.PawnStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchPawnRequest {
    private List<PawnStatus> pawnStatuses;
    private Long customerId;
    private Long pawnId;
    private String searchWord;
    private String customerName;
    private String itemName;
    private Long pawnAmount;
    private DateFilterRequest pawnDueDate;
    private DateFilterRequest redeemDate;
    private DateFilterRequest pawnDate;
    private DateFilterRequest forfeitedDate;
    private DateFilterRequest requestDate;
}
