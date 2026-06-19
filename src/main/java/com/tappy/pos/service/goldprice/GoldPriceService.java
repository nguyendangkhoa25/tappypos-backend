package com.tappy.pos.service.goldprice;

import com.tappy.pos.model.dto.goldprice.GoldPriceDTO;
import com.tappy.pos.model.dto.goldprice.PriceBoardResponse;

import java.util.List;

public interface GoldPriceService {
    List<GoldPriceDTO> getAllPrices();
    GoldPriceDTO createPrice(GoldPriceDTO dto);
    GoldPriceDTO updatePrice(Long id, GoldPriceDTO dto);
    void deletePrice(Long id);

    /** Shop's own gold-price snapshots over a trailing window (for the price-history chart). */
    java.util.List<com.tappy.pos.model.dto.tenant.GoldPriceHistoryDTO> getPriceHistory(int days);
    PriceBoardResponse getPriceBoard(String code);
    GoldPriceDTO getPriceForCategory(Long categoryId);

    /** Look up the current price row by its code (e.g. "B925", "B950"). */
    GoldPriceDTO getPriceByCode(String code);
}
