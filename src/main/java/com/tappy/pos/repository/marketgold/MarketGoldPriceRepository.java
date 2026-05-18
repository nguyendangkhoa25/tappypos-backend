package com.tappy.pos.repository.marketgold;

import com.tappy.pos.model.entity.marketgold.MarketGoldPrice;
import com.tappy.pos.model.entity.marketgold.MarketGoldPriceId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketGoldPriceRepository extends JpaRepository<MarketGoldPrice, MarketGoldPriceId> {
    List<MarketGoldPrice> findAllBySourceOrderByKtypeAsc(String source);
    List<MarketGoldPrice> findAllByOrderBySourceAscKtypeAsc();
}
