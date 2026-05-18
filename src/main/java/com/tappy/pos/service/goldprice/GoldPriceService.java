package com.tappy.pos.service.goldprice;

import com.tappy.pos.model.dto.goldprice.GoldPriceDTO;
import com.tappy.pos.model.dto.goldprice.PriceBoardResponse;

import java.util.List;

public interface GoldPriceService {
    List<GoldPriceDTO> getAllPrices();
    GoldPriceDTO createPrice(GoldPriceDTO dto);
    GoldPriceDTO updatePrice(Long id, GoldPriceDTO dto);
    void deletePrice(Long id);
    PriceBoardResponse getPriceBoard(String code);
    GoldPriceDTO getPriceForCategory(Long categoryId);
}
