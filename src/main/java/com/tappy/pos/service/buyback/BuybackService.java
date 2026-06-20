package com.tappy.pos.service.buyback;

import com.tappy.pos.model.dto.buyback.BuybackResponse;
import com.tappy.pos.model.dto.buyback.CreateBuybackRequest;
import com.tappy.pos.model.dto.buyback.SellBuybackRequest;
import com.tappy.pos.model.enums.BuybackStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Buyback (second-hand outright purchase) operations. See PAWN_BUYBACK_SPEC. */
public interface BuybackService {

    BuybackResponse createBuyback(CreateBuybackRequest request);

    BuybackResponse getBuyback(Long buybackId);

    Page<BuybackResponse> getBuybacks(BuybackStatus status, Pageable pageable);

    /** Marks a buyback SOLD with its final resale price; SOLD is terminal. */
    BuybackResponse markSold(Long buybackId, SellBuybackRequest request);

    /** Cancels a buyback (only before it is SOLD). */
    BuybackResponse cancelBuyback(Long buybackId, String reason);
}
