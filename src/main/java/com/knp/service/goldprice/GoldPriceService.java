package com.knp.service.goldprice;

import com.knp.model.dto.goldprice.GoldPriceDTO;
import com.knp.model.dto.goldprice.PriceBoardResponse;

import java.util.List;

public interface GoldPriceService {
    List<GoldPriceDTO> getAllPrices();
    GoldPriceDTO updatePrice(Long id, GoldPriceDTO dto);
    PriceBoardResponse getPriceBoard(String code);
}
