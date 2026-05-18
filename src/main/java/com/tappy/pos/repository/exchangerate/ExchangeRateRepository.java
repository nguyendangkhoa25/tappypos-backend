package com.tappy.pos.repository.exchangerate;

import com.tappy.pos.model.entity.exchangerate.ExchangeRate;
import com.tappy.pos.model.entity.exchangerate.ExchangeRateId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, ExchangeRateId> {

    List<ExchangeRate> findAllBySource(String source);
}
