package com.tappy.pos.service.recipe;

import com.tappy.pos.model.dto.recipe.IngredientConsumptionDTO;
import com.tappy.pos.model.dto.recipe.ProduceRequest;
import com.tappy.pos.model.dto.recipe.ProductionBatchDTO;
import com.tappy.pos.model.dto.recipe.ProductionSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ProductionService {

    /** Run a production batch: deduct ingredient stock, add finished-goods stock, snapshot cost. */
    ProductionBatchDTO produce(ProduceRequest request);

    /** Mark a COMPLETED batch as spoiled → write off the produced finished-goods stock. */
    ProductionBatchDTO markSpoiled(Long batchId);

    Page<ProductionBatchDTO> listBatches(LocalDate from, LocalDate to, Pageable pageable);

    /** Ingredient consumption over a period (tiêu hao nguyên liệu), reconstructed from recipes. */
    List<IngredientConsumptionDTO> getConsumption(LocalDate from, LocalDate to);

    /** Production totals over a period (mẻ sản xuất) + today's figures. */
    ProductionSummaryDTO getSummary(LocalDate from, LocalDate to);
}
