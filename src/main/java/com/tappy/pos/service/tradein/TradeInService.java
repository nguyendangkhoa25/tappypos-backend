package com.tappy.pos.service.tradein;

import com.tappy.pos.model.dto.tradein.CreateTradeInRequest;
import com.tappy.pos.model.dto.tradein.TradeInDTO;
import com.tappy.pos.model.enums.TradeInStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TradeInService {
    TradeInDTO create(CreateTradeInRequest request);
    TradeInDTO getById(Long id);
    Page<TradeInDTO> search(TradeInStatus status, Pageable pageable);
    TradeInDTO cancel(Long id, String reason);
}
